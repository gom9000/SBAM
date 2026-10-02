package net.gommagomma.sbam.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un filo fisico che collega uno o più pin, con la sua capacità verso massa.
 *
 * Fili e pin collegati tra loro formano un nodo elettrico (Node), ricavato dalla rete
 * quando viene compilata.
 */
public final class Wire
{
    private final String name;
    private final double capacitance;
    private final List<Pin> pins = new ArrayList<>();

    /** Il nodo a cui appartiene, assegnato dalla compilazione della rete. */
    Node node;

    /**
     * @param name        nome del filo
     * @param capacitance capacità propria verso massa (cablaggio, piste) [F]
     */
    public Wire(String name, double capacitance)
    {
        this.name = name;
        this.capacitance = capacitance;
    }

    void attach(Pin pin)
    {
        if (node != null) throw new IllegalStateException(name + ": rete già compilata, i collegamenti non cambiano più");
        pins.add(pin);
    }

    public String name()         { return name; }
    public List<Pin> pins()      { return Collections.unmodifiableList(pins); }

    /** Capacità propria del filo verso massa [F]. */
    public double capacitance()  { return capacitance; }

    /** Il nodo elettrico a cui il filo appartiene. */
    public Node node()
    {
        if (node == null) throw new IllegalStateException(name + ": rete non ancora compilata");
        return node;
    }

    /** Tensione assestata del nodo a cui il filo appartiene [V]. */
    public double volts()        { return node().volts(); }

    @Override
    public String toString() { return name; }
}
