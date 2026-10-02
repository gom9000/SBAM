package net.gommagomma.sbam.digital;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.InputStage;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Un pin digitale: il raccordo tra la fisica (tensioni) e la logica (livelli).
 *
 * È un Pin a tutti gli effetti: si collega ai fili, ha un nodo e una corrente. In più ha:
 *  - uno stadio d'ingresso e uno d'uscita (InputStage.NONE / OutputStage.NONE se mancano);
 *  - l'alimentazione del proprio device a cui fa riferimento (VDD e GND);
 *  - un'intenzione (Drive), decisa dalla logica;
 *  - il livello letto e il fronte, ricavati a ogni tick dalla tensione assestata.
 *
 * Il comportamento elettrico è tutto negli stadi: il pin delega, senza chiedersi di che tipo sono.
 */
public final class DigitalPin extends Pin
{
    private final DigitalDevice owner;
    private final Power power;
    private final InputStage input;
    private final OutputStage output;

    private Drive drive;                       // l'intenzione presente
    private Drive pending = null;              // la coda: al più un'intenzione in attesa del suo istante
    private long pendingAtPs = Long.MAX_VALUE;
    private Level level = Level.X;
    private Edge edge = Edge.NONE;
    private long edgePs = -1;
    private long changedPs = 0;

    DigitalPin(DigitalDevice owner, String name, Power power, InputStage input, OutputStage output)
    {
        super(owner, name, input.capacitance() + output.capacitance());
        if (power.vdd().device() != owner) throw new IllegalArgumentException(name + ": l'alimentazione deve essere dello stesso device");
        this.owner = owner;
        this.power = power;
        this.input = input;
        this.output = output;
        this.drive = output.initialDrive();
    }

    // ------------------------------------------------------------ per la logica (strato 3)

    /** Ciò che il pin ha letto alla fine dell'ultimo tick. */
    public Level level()         { return level; }

    /** Il fronte avvenuto nell'ultimo tick (NONE se il livello non è cambiato). */
    public Edge edge()           { return edge; }

    /** Istante dell'ultimo fronte [ps], -1 se non ce n'è ancora stato uno. */
    public long edgeTimePs()     { return edgePs; }

    /** Da quando il livello letto è quello attuale [ps]: l'istante dell'ultimo cambiamento, anche verso X. */
    public long stableSincePs()  { return changedPs; }

    /** L'intenzione presente dell'uscita. */
    public Drive driven()        { return drive; }

    /** L'intenzione verso cui l'uscita sta andando: quella in attesa, o la presente se non ce n'è. */
    public Drive target()        { return pending != null ? pending : drive; }

    /**
     * Chiede all'uscita un'intenzione, subito: diventa attiva dal tick successivo, e annulla
     * quella in attesa. Lecito solo nella logica del device (o in costruzione, per lo stato iniziale),
     * e solo se lo stadio d'uscita la accetta.
     */
    public void drive(Drive d)
    {
        accept(d);
        drive = d;
        pending = null;
    }

    /**
     * Chiede all'uscita un'intenzione da un certo istante [ps] (un ritardo).
     * L'intenzione arriva e attende il suo tick; la coda è di un solo elemento, e una decisione nuova
     * sostituisce quella in attesa: vince l'ultima (un impulso più breve del ritardo non arriva all'uscita).
     * Se è già l'intenzione verso cui si sta andando, non cambia nulla (il ritardo in corso continua).
     */
    public void drive(Drive d, long atPs)
    {
        accept(d);
        if (d == target()) return;
        pending = d == drive ? null : d;
        pendingAtPs = atPs;
    }

    /** Come drive(d, t), con il ritardo che i tempi dell'uscita danno per questa transizione. */
    public void drive(Drive d, long nowPs, Timing timing)
    {
        drive(d, nowPs + timing.delay(target(), d));
    }

    /**
     * Il livello letto come "basso o no", per un ingresso di controllo: X (durante un fronte)
     * non cambia la decisione presa prima.
     */
    public boolean isLow(boolean before)
    {
        return level == Level.X ? before : level == Level.L;
    }

    /** Come isLow, per "alto o no". */
    public boolean isHigh(boolean before)
    {
        return level == Level.X ? before : level == Level.H;
    }

    private void accept(Drive d)
    {
        owner.requireLogic();
        if (!output.accepts(d)) {
            throw new IllegalArgumentException(this + ": lo stadio d'uscita (" + output + ") non accetta " + d);
        }
    }

    // ------------------------------------------------------------ per lo strato 2 e le sentinelle

    public Power power()         { return power; }
    public InputStage input()    { return input; }
    public OutputStage output()  { return output; }

    // ------------------------------------------------------------ chiamati dal DigitalDevice

    /** Durante l'assestamento: dichiara la caratteristica di uscita e ingresso. Puro. */
    void react(Voltages trial, Reaction out)
    {
        output.react(this, power, drive, trial, out);
        input.react(this, power, trial, out);
    }

    /** Dopo la logica: se è arrivato il suo tick, l'intenzione in attesa diventa presente. */
    void applyDue(Tick tick)
    {
        if (pending == null || tick.next() < pendingAtPs) return;
        drive = pending;
        pending = null;
    }

    /** Dopo l'assestamento: legge il livello e ricava il fronte. */
    void sense(Tick tick, Voltages settled)
    {
        double gnd = settled.volts(power.gnd());
        Level now = input.read(settled.volts(this) - gnd, settled.volts(power.vdd()) - gnd, level);
        edge = now == level ? Edge.NONE
             : now == Level.H ? Edge.RISING
             : now == Level.L ? Edge.FALLING
             : Edge.NONE;
        if (edge != Edge.NONE) edgePs = tick.next();
        if (now != level) changedPs = tick.next();
        level = now;
    }
}
