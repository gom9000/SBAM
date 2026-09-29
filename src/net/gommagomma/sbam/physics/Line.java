package net.gommagomma.sbam.physics;


import net.gommagomma.sbam.engine.Log;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Una linea fisica con i pin collegati, un eventuale pull e la capacità del cablaggio.
 *
 * STATICA - a ogni passo calcola la tensione "di arrivo" con il teorema di Millman:
 *
 *        sum(Vi / Ri) + sum(correnti iniettate)                     1
 *   Vt = ----------------------------------------        Req = -----------
 *                   sum(1 / Ri)                                 sum(1 / Ri)
 *
 *   - ogni uscita che pilota:  generatore Vi con resistenza Ri (vedi OutputSpec)
 *   - pull-up / pull-down:     la tensione di una Rail oppure 0 V, attraverso la resistenza di pull
 *   - livello alto delle uscite e soglie CMOS: dalla Rail di ciascun pin
 *   - ingressi:                generatori di corrente (IIH / IIL)
 *
 * DINAMICA - la tensione vera non salta a Vt: la capacità della linea (cablaggio + pin)
 * si carica attraverso Req, con costante di tempo tau = Req * C:
 *
 *   V(t + dt) = Vt + (V(t) - Vt) * exp(-dt / tau)
 *
 *   ohm * pF = ps, quindi tau esce direttamente in picosecondi.
 *
 * FLOTTANTE - se nessuno pilota e non c'è pull, la carica resta sulla capacità:
 * la linea conserva l'ultima tensione e i pin continuano a leggerla (e a fidarsi, sbagliando).
 *
 * ASSORBIMENTO - la corrente che esce dalle alimentazioni (uscite alte, pull-up, e la carica
 * della capacità durante i fronti di salita) viene sommata sulla Rail di provenienza.
 *
 * DIODI DI PROTEZIONE - se la tensione supererebbe la rail di un ingresso di 0,6 V, il suo diodo
 * conduce: entra nel calcolo come un generatore (rail + 0,6 V) con una piccola resistenza, e la
 * corrente finisce NELLA rail di quel pin (che può anche essere spenta: la alimenta lui).
 *
 * Poi ogni pin legge la tensione con le proprie soglie.
 */
public class Line
{
    private final String name;
    private final List<Pin> pins = new ArrayList<>();

    private double wirePF = 0.0;
    private Level pullLevel = null;      // H = pull-up, L = pull-down, null = nessuno
    private Rail pullRail = null;        // verso quale alimentazione va il pull-up
    private double pullOhms = 0.0;
    private long slowEdgePs = 20_000;    // un ingresso in zona indefinita più a lungo di così: avviso

    private boolean initialized = false;
    private double volts = 0.0;          // tensione reale
    private double targetVolts = 0.0;    // dove sta andando (Millman)
    private double tauPs = 0.0;
    private boolean floating = true;
    private boolean conflict = false;
    private double pullCurrentMA = 0.0;
    private final List<Pin> clampHigh = new ArrayList<>();   // pin con il diodo verso la rail in conduzione
    private final List<Pin> clampLow = new ArrayList<>();    // pin con il diodo verso massa in conduzione

    // condizioni anomale: da quando sono attive, e se sono già state segnalate
    private final Map<String, Long> activeSince = new LinkedHashMap<>();
    private final Set<String> reported = new HashSet<>();


    public Line(String name)
    {
        this.name = name;
    }

    // ---------------------------------------------------------------- configurazione

    public Line connect(Pin pin)          { pins.add(pin); return this; }
    public Line wire(double pf)           { this.wirePF = pf; return this; }
    public Line pullUp(Rail rail, double ohms) { pullLevel = Level.H; pullRail = rail; pullOhms = ohms; return this; }
    public Line pullDown(double ohms)          { pullLevel = Level.L; pullRail = null; pullOhms = ohms; return this; }
    public Line slowEdge(long ps)         { this.slowEdgePs = ps; return this; }

    // ---------------------------------------------------------------- fase 2

    /**
     * Risolve le dichiarazioni del passo e fa evolvere la tensione per dtPs.
     * Al primo passo la linea parte già a regime (come dopo l'accensione).
     */
    public void resolve(long nowPs, long dtPs, Log log)
    {
        // 1) chi pilota, e sono d'accordo?
        Level first = null;
        conflict = false;
        for (Pin p : pins) {
            if (!p.isDriving()) continue;
            if (first == null) first = p.getDeclared();
            else if (first != p.getDeclared()) conflict = true;
        }

        // 2) statica: tensione di arrivo, con i diodi di protezione che entrano in conduzione se serve
        double oldVolts = volts;
        boolean firstStep = !initialized;
        int iterations = initialized ? 1 : 3;     // al primo passo la linea parte già a regime
        for (int k = 0; k < iterations; k++) {
            computeTarget();
            if (floating) break;
            // 3) dinamica: la capacità si carica verso la tensione di arrivo
            if (!initialized || tauPs == 0.0) {
                volts = targetVolts;
            } else {
                volts = targetVolts + (volts - targetVolts) * Math.exp(-dtPs / tauPs);
            }
        }
        // se flottante: volts resta dov'era (la carica rimane sulla capacità)
        initialized = true;

        // 4) corrente scambiata con le alimentazioni in questo passo, calcolata sulla tensione media
        //    del passo (comprende la carica della capacità durante i fronti)
        if (!floating) {
            double vAvg = firstStep ? volts : (oldVolts + volts) / 2.0;
            for (Pin p : pins) {
                if (p.getDeclared() == Level.H) {
                    double iMA = (theveninVolts(p) - vAvg) / theveninOhms(p) * 1000.0;
                    if (iMA > 0) p.getRail().addLoad(iMA);
                }
            }
            if (pullLevel == Level.H) {
                double iMA = (pullRail.getVolts() - vAvg) / pullOhms * 1000.0;
                if (iMA > 0) pullRail.addLoad(iMA);
            }
            for (Pin p : clampHigh) {
                // la corrente del diodo ENTRA nella rail del pin: carico negativo
                double iMA = (vAvg - clampHighVolts(p)) / InputSpec.CLAMP_OHMS * 1000.0;
                if (iMA > 0) p.getRail().addLoad(-iMA);
            }
        }

        // 5) correnti a regime, letture dei pin
        pullCurrentMA = pullLevel == null ? 0.0 : (sourceVolts(pullLevel) - targetVolts) / pullOhms * 1000.0;
        for (Pin p : pins) {
            double iMA = p.isDriving() ? (theveninVolts(p) - targetVolts) / theveninOhms(p) * 1000.0 : 0.0;
            p.update(volts, iMA);
        }

        // 6) condizioni anomale: la linea le manifesta quando iniziano e quando finiscono
        Map<String, String> now = new LinkedHashMap<>();
        Map<String, Long> minDuration = new LinkedHashMap<>();

        if (conflict) {
            put(now, minDuration, "conflict", 0, "KABOOM! conflitto tra " + describeDrivers()
                    + ", corrente di scontro " + fmt(maxDriverCurrentMA()) + " mA");
        }
        for (Pin p : pins) {
            if (p.isDriving()) {
                double rated = p.getOutput().ratedMA(p.getDeclared());
                if (Math.abs(p.getCurrentMA()) > rated * 1.0001) {
                    put(now, minDuration, "spec:" + p.getName(), 0, "fuori specifica: " + p.getName() + " "
                            + fmt(Math.abs(p.getCurrentMA())) + " mA, il datasheet garantisce " + fmt(rated) + " mA");
                }
            }
            if (clampHigh.contains(p)) {
                double iMA = (targetVolts - clampHighVolts(p)) / InputSpec.CLAMP_OHMS * 1000.0;
                put(now, minDuration, "clamp:" + p.getName(), 0, "diodo di protezione in conduzione: " + p.getName()
                        + " spinge " + fmt(iMA) + " mA nella rail " + p.getRail().getName()
                        + " (linea " + fmt(targetVolts) + " V, rail " + fmt(p.getRail().getVolts()) + " V)");
            }
            if (clampLow.contains(p)) {
                put(now, minDuration, "clampL:" + p.getName(), 0, "diodo di protezione verso massa in conduzione: "
                        + p.getName() + " (linea " + fmt(targetVolts) + " V)");
            }
            if (p.canRead() && floating) {
                put(now, minDuration, "float:" + p.getName(), 0, "linea flottante letta da " + p.getName()
                        + ": legge " + p.getSeen() + " dalla carica residua (" + fmt(volts) + " V)");
            }
            if (p.canRead() && p.getSeen() == Level.X) {
                put(now, minDuration, "undef:" + p.getName(), slowEdgePs, "livello non valido per " + p.getName()
                        + " (" + p.getInput().name() + ") da oltre " + Log.formatTime(slowEdgePs)
                        + ": " + fmt(volts) + " V tra VIL " + fmt(p.getInput().vilVolts(p.getRail().getVolts()))
                        + " e VIH " + fmt(p.getInput().vihVolts(p.getRail().getVolts())));
            }
        }
        track(nowPs, dtPs, now, minDuration, log);
    }

    private static void put(Map<String, String> now, Map<String, Long> min, String key, long minPs, String msg)
    {
        now.put(key, msg);
        min.put(key, minPs);
    }

    /** Segnala una condizione quando dura abbastanza, e la sua fine con la durata. */
    private void track(long nowPs, long dtPs, Map<String, String> now, Map<String, Long> minDuration, Log log)
    {
        for (Map.Entry<String, String> c : now.entrySet()) {
            String key = c.getKey();
            activeSince.putIfAbsent(key, nowPs);
            long since = activeSince.get(key);
            if (!reported.contains(key) && nowPs + dtPs - since > minDuration.get(key)) {
                log.add(since, name, c.getValue());
                reported.add(key);
            }
        }
        for (String key : new ArrayList<>(activeSince.keySet())) {
            if (now.containsKey(key)) continue;
            long since = activeSince.remove(key);
            if (reported.remove(key)) {
                log.add(nowPs, name, "fine " + describeKey(key) + ", durata " + Log.formatTime(nowPs - since));
            }
        }
    }

    private static String describeKey(String key)
    {
        String[] parts = key.split(":", 2);
        String what = switch (parts[0]) {
            case "conflict" -> "conflitto";
            case "spec" -> "fuori specifica";
            case "float" -> "linea flottante";
            case "undef" -> "livello non valido";
            case "clamp", "clampL" -> "diodo di protezione";
            default -> parts[0];
        };
        return parts.length > 1 ? what + " (" + parts[1] + ")" : what;
    }

    // ---------------------------------------------------------------- calcoli

    /**
     * Calcola la tensione di arrivo (Millman) e la costante di tempo.
     * I diodi di protezione sono non lineari: si parte senza, e si accendono quelli la cui soglia
     * verrebbe superata, ricalcolando (approssimazione: la decisione si basa sulla tensione di arrivo).
     */
    private void computeTarget()
    {
        clampHigh.clear();
        clampLow.clear();
        for (int round = 0; round < 4; round++) {
            double g = conductance();
            floating = g == 0.0;
            if (floating) return;

            double injectedMA = 0.0;
            for (Pin p : pins) {
                if (p.canRead()) {
                    // la corrente d'ingresso dipende da come il pin legge ADESSO la linea
                    injectedMA -= p.getInput().currentMA(p.wouldRead(volts));
                }
            }
            targetVolts = (sourceCurrent() + injectedMA / 1000.0) / g;
            tauPs = (1.0 / g) * getCapacitancePF();

            boolean changed = false;
            for (Pin p : pins) {
                if (!p.canRead() || !p.getInput().clamp()) continue;
                if (targetVolts > clampHighVolts(p) && !clampHigh.contains(p)) { clampHigh.add(p); changed = true; }
                if (targetVolts < -InputSpec.CLAMP_VF && !clampLow.contains(p)) { clampLow.add(p); changed = true; }
            }
            if (!changed) return;
        }
    }

    private static double clampHighVolts(Pin p)
    {
        return p.getRail().getVolts() + InputSpec.CLAMP_VF;
    }

    private double conductance()
    {
        double g = 0.0;
        for (Pin p : pins) if (p.isDriving()) g += 1.0 / theveninOhms(p);
        if (pullLevel != null) g += 1.0 / pullOhms;
        g += (clampHigh.size() + clampLow.size()) / InputSpec.CLAMP_OHMS;
        return g;
    }

    private double sourceCurrent()
    {
        double i = 0.0;
        for (Pin p : pins) if (p.isDriving()) i += theveninVolts(p) / theveninOhms(p);
        if (pullLevel != null) i += sourceVolts(pullLevel) / pullOhms;
        for (Pin p : clampHigh) i += clampHighVolts(p) / InputSpec.CLAMP_OHMS;
        for (Pin p : clampLow) i += -InputSpec.CLAMP_VF / InputSpec.CLAMP_OHMS;
        return i;
    }

    private double theveninVolts(Pin p)
    {
        return p.getDeclared() == Level.H ? p.getOutput().highVolts(p.getRail().getVolts()) : 0.0;
    }

    private double theveninOhms(Pin p)
    {
        return p.getDeclared() == Level.H ? p.getOutput().rHigh() : p.getOutput().rLow();
    }

    private double sourceVolts(Level level)
    {
        return level == Level.H ? pullRail.getVolts() : 0.0;
    }

    private double maxDriverCurrentMA()
    {
        double max = 0.0;
        for (Pin p : pins) if (p.isDriving()) max = Math.max(max, Math.abs(p.getCurrentMA()));
        return max;
    }

    private String describeDrivers()
    {
        StringBuilder sb = new StringBuilder();
        for (Pin p : getDrivers()) {
            if (!sb.isEmpty()) sb.append(" e ");
            sb.append(p.getName()).append("=").append(p.getDeclared());
        }
        return sb.toString();
    }

    static String fmt(double v)
    {
        return String.format(Locale.ITALIAN, "%.2f", v);
    }

    // ---------------------------------------------------------------- stato

    public List<Pin> getDrivers()
    {
        List<Pin> drivers = new ArrayList<>();
        for (Pin p : pins) if (p.isDriving()) drivers.add(p);
        return drivers;
    }

    /** Capacità totale: cablaggio + tutti i pin collegati [pF]. */
    public double getCapacitancePF()
    {
        double c = wirePF;
        for (Pin p : pins) c += p.getCapacitancePF();
        return c;
    }

    public String getName()             { return name; }
    public double getVolts()            { return volts; }
    public double getTargetVolts()      { return targetVolts; }
    /** Costante di tempo attuale [ps]: quanto è "veloce" la linea adesso. */
    public double getTauPs()            { return tauPs; }
    public boolean isFloating()         { return floating; }
    public boolean isConflict()         { return conflict; }
    /** Corrente a regime nel resistore di pull [mA] (positiva = dal pull verso la linea). */
    public double getPullCurrentMA()    { return pullCurrentMA; }
    public List<Pin> getPins()          { return List.copyOf(pins); }
}
