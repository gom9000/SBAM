package net.gommagomma.sbam.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * Un gruppo ordinato di fili sulla scheda: un bus dati, un bus indirizzi.
 *
 * Non ha un valore suo: ogni filo ha una tensione, e la parola esiste solo per chi la legge
 * con le proprie soglie (una porta). È il posto dove un domani mettere la fisica tra fili
 * vicini (accoppiamento, diafonia).
 */
public final class Bus extends Group<Wire>
{
    public Bus(String name, List<Wire> wires)
    {
        super(name, wires);
    }

    /** Un bus di fili nuovi, chiamati name0, name1, ... con la stessa capacità [F]. */
    public static Bus of(String name, int width, double capacitance)
    {
        List<Wire> wires = new ArrayList<>(width);
        for (int i = 0; i < width; i++) wires.add(new Wire(name + i, capacitance));
        return new Bus(name, wires);
    }

    /** I fili da from (compreso) a to (escluso), come un bus a sé. */
    public Bus slice(int from, int to)
    {
        return new Bus(sliceName(from, to), range(from, to));
    }
}
