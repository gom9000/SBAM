package net.gommagomma.sbam.instrument;

import net.gommagomma.sbam.physics.Tick;

/**
 * Un segnale: una grandezza del circuito che uno strumento osserva nel tempo.
 * Ha un nome, e appartiene a un gruppo (il device del pin, "wires" per i fili).
 *
 * Il segnale sa che tipo di valore ha, e lo consegna a una Capture chiamando il metodo giusto:
 * chi registra (traccia, VCD, analizzatore logico, GUI) non deve chiedersi di che segnale si tratta.
 * Osserva e basta: non modifica nulla.
 */
public abstract class Signal
{
    private final String group;
    private final String name;

    protected Signal(String group, String name)
    {
        this.group = group;
        this.name = name;
    }

    public final String group()     { return group; }
    public final String name()      { return name; }

    /** "gruppo.nome", per esempio "U1.Y.drive" o "wires.BUS0". */
    public final String fullName()  { return group + "." + name; }

    /** Si presenta alla capture, prima della registrazione: nome, tipo, larghezza. */
    public abstract void declare(Capture capture);

    /** Consegna alla capture il valore alla fine del tick. */
    public abstract void sample(Tick tick, Capture capture);

    @Override
    public String toString() { return fullName(); }
}
