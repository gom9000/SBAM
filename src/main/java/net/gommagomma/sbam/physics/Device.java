package net.gommagomma.sbam.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tutto ciò che agisce sui fili, attraverso i suoi pin.
 *
 * Passivo: ha solo pin di segnale (resistenza, diodo, condensatore).
 * Attivo:  ha anche pin sui fili di alimentazione (buffer, RAM, PIC, alimentatore);
 *          tutto ciò che riguarda l'alimentazione resta incapsulato nel device.
 *
 * A ogni tick la rete si assesta: propone delle tensioni di prova e ogni device reagisce
 * con la caratteristica dei suoi pin (react), finché tutto torna. Poi ogni device legge
 * le tensioni assestate e aggiorna il proprio stato (update).
 *
 * React e update li chiama solo la rete.
 */
public abstract class Device
{
    /** Il momento in cui si trova il device. */
    public enum Phase
    {
        /** In costruzione: si creano pin e collegamenti. */
        BUILD,
        /** La rete si sta assestando: si risponde e basta. */
        SETTLE,
        /** La rete è assestata: si legge e si decide. */
        UPDATE,
        /** Tra un momento e l'altro. */
        IDLE
    }

    private final String name;
    private final List<Pin> pins = new ArrayList<>();
    private Phase phase = Phase.BUILD;

    protected Device(String name)
    {
        this.name = name;
    }

    public final String name()     { return name; }
    public final List<Pin> pins()  { return Collections.unmodifiableList(pins); }

    // ------------------------------------------------------------ costruzione

    /** Crea un pin semplice di questo device. Solo in costruzione. */
    protected final Pin pin(String pinName, double capacitance)
    {
        return add(new Pin(this, pinName, capacitance));
    }

    /** Crea una porta di pin semplici, chiamati name0, name1, ... Solo in costruzione. */
    protected final Port<Pin> port(String portName, int width, double capacitance)
    {
        List<Pin> ps = new ArrayList<>(width);
        for (int i = 0; i < width; i++) ps.add(pin(portName + i, capacitance));
        return new Port<>(portName, ps);
    }

    /** Aggiunge a questo device un pin già creato (per esempio una specializzazione). Solo in costruzione. */
    protected final <P extends Pin> P add(P pin)
    {
        require(Phase.BUILD);
        if (pin.device() != this) throw new IllegalArgumentException(pin + " appartiene a un altro device");
        pins.add(pin);
        return pin;
    }

    // ------------------------------------------------------------ ciò che implementa un device

    /**
     * La rete, durante l'assestamento, chiede: "se le tensioni fossero queste, come reagiresti?".
     * Il device dichiara la caratteristica dei suoi pin in out.
     *
     * Può essere chiamato più volte nello stesso tick: si risponde e basta.
     * Mentre la rete si assesta non si sa ancora cosa c'è sulle linee, quindi non si decide nulla:
     * nessuno stato modificato. Le decisioni si prendono in update().
     */
    protected abstract void react(Tick tick, Voltages trial, Reaction out);

    /**
     * La rete è assestata: il device legge le tensioni e aggiorna il proprio stato
     * (memoria fisica, logica, programma, intenzioni per il tick successivo).
     * Chiamato una volta per tick.
     */
    protected abstract void update(Tick tick, Voltages settled);

    // ------------------------------------------------------------ vincoli per gli strati superiori

    protected final Phase phase() { return phase; }

    /** Lancia un errore se un'operazione viene fatta nel momento sbagliato. */
    protected final void require(Phase expected)
    {
        if (phase != expected) {
            throw new IllegalStateException(name + ": operazione permessa solo in " + expected + ", non in " + phase);
        }
    }

    // ------------------------------------------------------------ usati solo dalla rete (stesso package)

    /** La rete è stata compilata: finita la costruzione. */
    final void built()
    {
        phase = Phase.IDLE;
    }

    final void doReact(Tick tick, Voltages trial, Reaction out)
    {
        phase = Phase.SETTLE;
        try {
            react(tick, trial, out);
        } finally {
            phase = Phase.IDLE;
        }
    }

    final void doUpdate(Tick tick, Voltages settled)
    {
        phase = Phase.UPDATE;
        try {
            update(tick, settled);
        } finally {
            phase = Phase.IDLE;
        }
    }

    @Override
    public String toString() { return name; }
}
