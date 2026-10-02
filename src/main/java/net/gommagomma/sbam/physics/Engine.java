package net.gommagomma.sbam.physics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Il motore: fa scorrere il tempo a tick e governa la rete.
 *
 * Ciclo di vita:
 *   costruire  - si creano i device, i fili e i collegamenti, e si aggiungono i device al motore
 *   compilare  - il motore ricava i nodi dai collegamenti e blocca la topologia (alla prima esecuzione)
 *   eseguire   - tick dopo tick: la rete si assesta, ogni device aggiorna il proprio stato,
 *                gli strumenti (Instrument) osservano
 *
 * Assestamento di un tick, per ogni nodo (legge di Kirchhoff delle correnti):
 *   le correnti dei pin, più quella che carica la capacità del nodo, si annullano.
 * La capacità C del nodo equivale a un generatore pari alla tensione del tick precedente,
 * con resistenza in serie dt / C: è l'inerzia che rende graduali i fronti, e che tiene la carica
 * su un nodo flottante.
 * La rete propone tensioni di prova, chiede a tutti i device la caratteristica dei loro pin,
 * ricalcola le tensioni e ripete finché smettono di cambiare.
 *
 * All'avvio tutti i nodi sono a 0 V.
 */
public final class Engine
{
    private final long stepPs;
    private long nowPs = 0;

    private final List<Device> devices = new ArrayList<>();
    private boolean compiled = false;

    private double toleranceVolts = 1e-6;
    private int maxIterations = 50;
    private boolean checkPurity = false;
    private boolean printWarnings = true;
    private final List<String> warnings = new ArrayList<>();   // i primi MAX_WARNINGS avvisi
    private static final int MAX_WARNINGS = 20;

    private int lastIterations = 0;
    private double lastResidual = 0.0;
    private long unsettledTicks = 0;

    // ------------------------------------------------------------ rete compilata
    private List<Node> nodes = List.of();
    private double[] capacitance;   // capacità di ogni nodo [F]
    private double[] settled;       // tensioni assestate (stato vero), letto anche dai Node
    private boolean[] floating;     // nodi non pilotati da nessuno, letto anche dai Node
    private double[] trial;         // tensioni di prova del tentativo in corso
    private double[] next;          // tensioni calcolate dal tentativo in corso
    private double[] sumG;          // somma delle conduttanze dei pin [S]
    private double[] sumI;          // somma delle correnti equivalenti dei pin [A]
    private double[] pinG;          // per pin: somma delle conduttanze dichiarate [S]
    private double[] pinI;          // per pin: somma delle correnti equivalenti dichiarate [A]

    private final List<Instrument> instruments = new ArrayList<>();
    private boolean observing = false;

    private final Collector collector = new Collector();
    private final Voltages trialView = new TrialVoltages();
    private final Voltages settledView = new SettledVoltages();

    /** @param stepPs durata del tick [ps] */
    public Engine(long stepPs)
    {
        if (stepPs <= 0) throw new IllegalArgumentException("la durata del tick deve essere positiva");
        this.stepPs = stepPs;
    }

    // ------------------------------------------------------------ costruzione e opzioni

    /** Aggiunge uno strumento: viene chiamato alla fine di ogni tick. */
    public <I extends Instrument> I add(I instrument)
    {
        requireNotObserving();
        instruments.add(instrument);
        return instrument;
    }

    /** Aggiunge un device alla simulazione. Solo prima della compilazione. */
    public <D extends Device> D add(D device)
    {
        requireNotObserving();
        if (compiled) throw new IllegalStateException("rete già compilata: non si aggiungono device");
        for (Device d : devices) {
            if (d == device) throw new IllegalArgumentException(device + " aggiunto due volte");
        }
        devices.add(device);
        return device;
    }

    /** Scarto massimo tra due tentativi perché la rete si consideri assestata [V]. */
    public Engine tolerance(double volts)          { this.toleranceVolts = volts; return this; }

    /** Numero massimo di tentativi per tick. */
    public Engine maxIterations(int n)             { this.maxIterations = n; return this; }

    /**
     * Controllo di purezza: a ogni tentativo chiede due volte la stessa cosa a ogni device e
     * verifica che risponda allo stesso modo. Per lo sviluppo dei device: raddoppia il costo.
     */
    public Engine checkPurity(boolean enabled)     { this.checkPurity = enabled; return this; }

    /** Se stampare gli avvisi su System.err mentre accadono (default: sì). Restano comunque in warnings(). */
    public Engine printWarnings(boolean enabled)   { this.printWarnings = enabled; return this; }

    // ------------------------------------------------------------ stato

    public long nowPs()          { return nowPs; }

    /** Tentativi usati dall'ultimo assestamento. */
    public int lastIterations()  { return lastIterations; }

    /** Scarto massimo tra gli ultimi due tentativi dell'ultimo assestamento [V]. */
    public double lastResidual() { return lastResidual; }

    /** Quanti tick finora si sono chiusi senza raggiungere la tolleranza. */
    public long unsettledTicks() { return unsettledTicks; }
    public long stepPs()         { return stepPs; }

    /** Gli avvisi del motore (i primi venti). */
    public List<String> warnings() { return Collections.unmodifiableList(warnings); }
    public List<Device> devices(){ return Collections.unmodifiableList(devices); }

    public List<Node> nodes()
    {
        compile();
        return nodes;
    }

    // ------------------------------------------------------------ compilazione

    /** Ricava i nodi dai collegamenti e blocca la topologia. Viene fatta da sola alla prima esecuzione. */
    public void compile()
    {
        if (compiled) return;

        // elenco di tutti i pin e di tutti i fili raggiungibili
        List<Pin> pins = new ArrayList<>();
        List<Wire> wires = new ArrayList<>();
        Map<Object, Integer> id = new IdentityHashMap<>();
        for (Device d : devices) {
            for (Pin p : d.pins()) {
                requireCapacitance(p.toString(), p.capacitance());
                id.put(p, id.size());
                pins.add(p);
            }
        }
        for (Pin p : pins) {
            for (Wire w : p.wires()) {
                if (!id.containsKey(w)) {
                    requireCapacitance(w.name(), w.capacitance());
                    id.put(w, id.size());
                    wires.add(w);
                }
            }
        }

        // pin e fili collegati finiscono nello stesso nodo
        int[] parent = new int[id.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (Pin p : pins) {
            for (Wire w : p.wires()) union(parent, id.get(p), id.get(w));
        }

        Map<Integer, List<Pin>> pinsOf = new HashMap<>();
        Map<Integer, List<Wire>> wiresOf = new HashMap<>();
        List<Integer> roots = new ArrayList<>();
        for (Pin p : pins) {
            int r = find(parent, id.get(p));
            if (!pinsOf.containsKey(r)) {
                roots.add(r);
                pinsOf.put(r, new ArrayList<>());
                wiresOf.put(r, new ArrayList<>());
            }
            pinsOf.get(r).add(p);
        }
        for (Wire w : wires) wiresOf.get(find(parent, id.get(w))).add(w);

        int n = roots.size();
        capacitance = new double[n];
        settled = new double[n];
        floating = new boolean[n];
        trial = new double[n];
        next = new double[n];
        sumG = new double[n];
        sumI = new double[n];

        pinG = new double[pins.size()];
        pinI = new double[pins.size()];
        for (int i = 0; i < pins.size(); i++) {
            Pin p = pins.get(i);
            p.index = i;
            p.conductances = pinG;
            p.currents = pinI;
        }

        List<Node> built = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int r = roots.get(i);
            Node node = new Node(i, wiresOf.get(r), pinsOf.get(r), settled, floating);
            if (!(node.capacitance() > 0.0)) {
                throw new IllegalStateException("nodo " + node + " senza capacità: ogni nodo deve avere una "
                        + "capacità verso massa maggiore di zero (fili o pin)");
            }
            capacitance[i] = node.capacitance();
            for (Pin p : node.pins()) p.node = node;
            for (Wire w : node.wires()) w.node = node;
            built.add(node);
        }
        nodes = Collections.unmodifiableList(built);

        for (Device d : devices) d.built();
        compiled = true;
    }

    private static void requireCapacitance(String what, double c)
    {
        if (Double.isNaN(c) || c < 0.0) {
            throw new IllegalStateException(what + ": capacità non valida (" + c + "), deve essere >= 0");
        }
    }

    private static int find(int[] parent, int i)
    {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b)
    {
        int ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent[ra] = rb;
    }

    // ------------------------------------------------------------ esecuzione

    /**
     * Esegue un tick: la rete si assesta, poi ogni device aggiorna il proprio stato,
     * poi gli strumenti osservano lo stato raggiunto (quello all'istante tick.next()).
     */
    public void step()
    {
        requireNotObserving();
        compile();
        Tick tick = new Tick(nowPs, stepPs);

        settle(tick);

        for (Device d : devices) d.doUpdate(tick, settledView);

        nowPs += stepPs;

        observing = true;
        try {
            for (Instrument i : instruments) i.observe(tick, this);
        } finally {
            observing = false;
        }
    }

    private void requireNotObserving()
    {
        if (observing) throw new IllegalStateException("uno strumento osserva e basta: non fa avanzare né modifica la simulazione");
    }

    /** Esegue tick finché il tempo non raggiunge l'istante dato [ps]. */
    public void runUntil(long timePs)
    {
        while (nowPs < timePs) step();
    }

    private void settle(Tick tick)
    {
        double dt = tick.seconds();
        int n = settled.length;
        System.arraycopy(settled, 0, trial, 0, n);   // primo tentativo: com'era al tick precedente

        double delta = Double.POSITIVE_INFINITY;
        int round = 0;
        while (round < maxIterations) {
            Arrays.fill(sumG, 0.0);
            Arrays.fill(sumI, 0.0);
            Arrays.fill(pinG, 0.0);
            Arrays.fill(pinI, 0.0);

            for (Device d : devices) ask(d, tick);

            delta = 0.0;
            for (int i = 0; i < n; i++) {
                double gc = capacitance[i] / dt;                        // inerzia della capacità
                double v = (sumI[i] + gc * settled[i]) / (sumG[i] + gc);
                if (Double.isNaN(v)) throw new IllegalStateException("tensione non definita sul nodo " + nodes.get(i));
                next[i] = v;
                delta = Math.max(delta, Math.abs(v - trial[i]));
            }
            double[] t = trial; trial = next; next = t;
            round++;
            if (delta <= toleranceVolts) break;
        }

        lastIterations = round;
        lastResidual = delta;
        if (delta > toleranceVolts) {
            unsettledTicks++;
            warn("tick " + tick.nowPs() + " ps: la rete non si è assestata in " + maxIterations
                    + " tentativi (scarto " + delta + " V)");
        }

        System.arraycopy(trial, 0, settled, 0, n);
        for (int i = 0; i < n; i++) floating[i] = sumG[i] == 0.0;
    }

    /** Chiede a un device la caratteristica dei suoi pin e la somma negli accumulatori dei nodi. */
    private void ask(Device d, Tick tick)
    {
        if (!checkPurity) {
            collector.asking = d;
            d.doReact(tick, trialView, collector);
            return;
        }
        Recorder first = new Recorder(d);
        Recorder second = new Recorder(d);
        d.doReact(tick, trialView, first);
        d.doReact(tick, trialView, second);
        if (!first.sameAs(second)) {
            throw new IllegalStateException(d + ": react() ha risposto in modo diverso alla stessa domanda. "
                    + "Durante l'assestamento si risponde e basta: le decisioni vanno in update()");
        }
        collector.asking = d;
        first.replayInto(collector);
    }

    private void warn(String message)
    {
        if (warnings.size() < MAX_WARNINGS) {
            String m = warnings.size() == MAX_WARNINGS - 1 ? message + " (altri avvisi soppressi)" : message;
            warnings.add(m);
            if (printWarnings) System.err.println("SBAM: " + m);
        }
    }

    // ------------------------------------------------------------ tensioni viste dai device

    /** Le tensioni del tentativo in corso, durante l'assestamento. */
    private final class TrialVoltages implements Voltages
    {
        @Override
        public double volts(Pin pin) { return trial[pin.node.index()]; }
    }

    /** Le tensioni assestate, durante l'update. */
    private final class SettledVoltages implements Voltages
    {
        @Override
        public double volts(Pin pin) { return settled[pin.node.index()]; }
    }

    // ------------------------------------------------------------ raccolta delle caratteristiche

    /** Controlla ogni dichiarazione e la somma nel nodo del pin: nessun oggetto creato. */
    private final class Collector implements Reaction
    {
        Device asking;

        @Override
        public void set(Pin pin, double volts, double resistance, double current)
        {
            if (pin.device() != asking) {
                throw new IllegalStateException(asking + " ha dichiarato la caratteristica di " + pin
                        + ": un device dichiara solo i propri pin");
            }
            if (Double.isNaN(resistance) || resistance <= 0.0) {
                throw new IllegalStateException(pin + ": resistenza in serie non valida (" + resistance + ")");
            }
            if (!Double.isFinite(volts) || !Double.isFinite(current)) {
                throw new IllegalStateException(pin + ": tensione o corrente non valida (" + volts + " V, " + current + " A)");
            }
            int i = pin.node.index();
            double g = 1.0 / resistance;
            double eq = volts * g + current;
            sumG[i] += g;
            sumI[i] += eq;
            pinG[pin.index] += g;
            pinI[pin.index] += eq;
        }
    }

    /** Per il controllo di purezza: registra le dichiarazioni di un device. */
    private static final class Recorder implements Reaction
    {
        private final Device device;
        private final List<Pin> pins = new ArrayList<>();
        private final List<double[]> values = new ArrayList<>();

        Recorder(Device device) { this.device = device; }

        @Override
        public void set(Pin pin, double volts, double resistance, double current)
        {
            pins.add(pin);
            values.add(new double[] { volts, resistance, current });
        }

        boolean sameAs(Recorder other)
        {
            if (pins.size() != other.pins.size()) return false;
            for (int i = 0; i < pins.size(); i++) {
                if (pins.get(i) != other.pins.get(i)) return false;
                if (!Arrays.equals(values.get(i), other.values.get(i))) return false;
            }
            return true;
        }

        void replayInto(Reaction target)
        {
            for (int i = 0; i < pins.size(); i++) {
                double[] v = values.get(i);
                target.set(pins.get(i), v[0], v[1], v[2]);
            }
        }

        @Override
        public String toString() { return "registrazione di " + device; }
    }
}
