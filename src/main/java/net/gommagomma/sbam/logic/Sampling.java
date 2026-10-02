package net.gommagomma.sbam.logic;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Edge;

import java.util.Collections;
import java.util.List;

/**
 * Un campionamento dichiarato da un device sincrono: su un fronte di un pin di clock, il device legge
 * certi pin di dato, e per leggerli bene questi devono essere stabili da almeno tsu prima del fronte
 * e restare fermi per almeno th dopo. Altrimenti il dato catturato non è garantito (metastabilità).
 *
 * È la dichiarazione di un vincolo, non un controllo: la controlla chi osserva (una sentinella).
 */
public final class Sampling
{
    private final String name;
    private final DigitalPin clock;
    private final Edge edge;
    private final List<DigitalPin> data;
    private final long setupPs;
    private final long holdPs;

    public Sampling(String name, DigitalPin clock, Edge edge, List<DigitalPin> data, long setupPs, long holdPs)
    {
        if (setupPs < 0 || holdPs < 0) throw new IllegalArgumentException(name + ": setup o hold negativi");
        this.name = name;
        this.clock = clock;
        this.edge = edge;
        this.data = List.copyOf(data);
        this.setupPs = setupPs;
        this.holdPs = holdPs;
    }

    public String name()            { return name; }
    public DigitalPin clock()       { return clock; }
    public Edge edge()              { return edge; }
    public List<DigitalPin> data()  { return Collections.unmodifiableList(data); }
    public long setupPs()           { return setupPs; }
    public long holdPs()            { return holdPs; }

    @Override
    public String toString() { return name; }
}
