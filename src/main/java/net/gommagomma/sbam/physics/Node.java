package net.gommagomma.sbam.physics;

import java.util.Collections;
import java.util.List;

/**
 * Un nodo elettrico: fili e pin collegati tra loro, con una sola tensione.
 *
 * Il filo è l'oggetto fisico, il nodo è l'oggetto elettrico.
 * Lo ricava la rete quando viene compilata, seguendo i collegamenti tra fili e pin.
 * Un pin senza fili forma un nodo da solo.
 */
public final class Node
{
    private final int index;
    private final String name;
    private final List<Wire> wires;
    private final List<Pin> pins;
    private final double capacitance;

    /** Tensioni assestate della rete, per indice di nodo: l'array è della rete, il nodo lo legge. */
    private final double[] settled;

    /** Nodi senza nessun pin che li pilota, per indice di nodo: anche questo array è della rete. */
    private final boolean[] floating;

    Node(int index, List<Wire> wires, List<Pin> pins, double[] settled, boolean[] floating)
    {
        this.index = index;
        this.wires = List.copyOf(wires);
        this.pins = List.copyOf(pins);
        this.settled = settled;
        this.floating = floating;
        this.name = !this.wires.isEmpty() ? this.wires.get(0).name() : this.pins.get(0).toString();

        double c = 0.0;
        for (Wire w : this.wires) c += w.capacitance();
        for (Pin p : this.pins) c += p.capacitance();
        this.capacitance = c;
    }

    /** Numero del nodo nella rete compilata: è l'indice usato negli array del calcolo. */
    int index()                  { return index; }

    /** Nome del nodo: quello del suo primo filo, o del pin se non ha fili. */
    public String name()         { return name; }

    public List<Wire> wires()    { return Collections.unmodifiableList(wires); }
    public List<Pin> pins()      { return Collections.unmodifiableList(pins); }

    /** Capacità totale verso massa: fili + pin [F]. */
    public double capacitance()  { return capacitance; }

    /** Tensione assestata [V]. */
    public double volts()        { return settled[index]; }

    /**
     * Vero se, nell'ultimo tick, nessun pin pilotava il nodo (tutti con resistenza infinita):
     * la tensione è solo la carica rimasta sulla capacità.
     */
    public boolean isFloating()  { return floating[index]; }

    @Override
    public String toString() { return name; }
}
