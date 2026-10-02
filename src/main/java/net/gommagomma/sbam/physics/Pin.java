package net.gommagomma.sbam.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interfaccia fisica tra un device ed uno o più fili collegati.
 *
 * Il suo comportamento elettrico lo dichiara il device in react(),
 * perché può dipendere da altri pin correlati dello stesso device.
 *
 * Si crea nel costruttore del device a cui appartiene, e si collega
 * prima che la rete venga compilata: dopo, i collegamenti non cambiano più.
 */
public class Pin
{
    private final Device device;
    private final String name;
    private final double capacitance;
    private final List<Wire> wires = new ArrayList<>();

    /** Il nodo a cui appartiene, assegnato dalla compilazione della rete. */
    Node node;

    /** Indice del pin nella rete compilata, e gli array della rete da cui ricavare la corrente. */
    int index = -1;
    double[] conductances;   // somma delle conduttanze dichiarate dal pin [S]
    double[] currents;       // somma delle correnti equivalenti dichiarate dal pin [A]

    protected Pin(Device device, String name, double capacitance)
    {
        this.device = device;
        this.name = name;
        this.capacitance = capacitance;
    }

    /** Collega un filo a questo pin. */
    public final Pin connect(Wire wire)
    {
        if (node != null) throw new IllegalStateException(this + ": rete già compilata, i collegamenti non cambiano più");
        wire.attach(this);
        wires.add(wire);
        return this;
    }

    public final String name()        { return name; }
    public final Device device()      { return device; }
    public final List<Wire> wires()   { return Collections.unmodifiableList(wires); }

    /** Capacità del pin verso massa [F]. */
    public final double capacitance() { return capacitance; }

    /** Il nodo elettrico a cui il pin appartiene. */
    public final Node node()
    {
        if (node == null) throw new IllegalStateException(this + ": rete non ancora compilata");
        return node;
    }

    /**
     * Corrente che il pin manda nel suo nodo, alla fine dell'ultimo tick [A]:
     * positiva se il pin eroga, negativa se assorbe.
     */
    public final double current()
    {
        Node n = node();
        return currents[index] - conductances[index] * n.volts();
    }

    @Override
    public String toString() { return device.name() + "." + name; }
}
