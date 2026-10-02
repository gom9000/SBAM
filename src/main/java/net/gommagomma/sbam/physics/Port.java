package net.gommagomma.sbam.physics;

import java.util.List;

/**
 * Un gruppo ordinato di pin dello stesso device: il lato del device di un bus.
 * Si collega a un bus della stessa larghezza, pin i sul filo i.
 * Gli strati superiori la estendono per i loro tipi di pin (vedi DigitalPort).
 */
public class Port<P extends Pin> extends Group<P>
{
    public Port(String name, List<P> pins)
    {
        super(name, pins);
        Device owner = pins.get(0).device();
        for (P p : pins) {
            if (p.device() != owner) throw new IllegalArgumentException(name + ": " + p + " è di un altro device");
        }
    }

    public final Device device() { return get(0).device(); }

    /** Collega ogni pin al filo corrispondente del bus. */
    public final Port<P> connect(Bus bus)
    {
        if (bus.width() != width()) {
            throw new IllegalArgumentException(this + " non si collega a " + bus + ": larghezze diverse");
        }
        for (int i = 0; i < width(); i++) get(i).connect(bus.get(i));
        return this;
    }

    /** Collega tutti i pin allo stesso filo (per esempio tutti a massa). */
    public final Port<P> connect(Wire wire)
    {
        for (P p : items()) p.connect(wire);
        return this;
    }

    /** I pin da from (compreso) a to (escluso), come una porta a sé. */
    public Port<P> slice(int from, int to)
    {
        return new Port<>(sliceName(from, to), range(from, to));
    }

    @Override
    public String toString() { return device().name() + "." + super.toString(); }
}
