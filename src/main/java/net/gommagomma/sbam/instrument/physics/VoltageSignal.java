package net.gommagomma.sbam.instrument.physics;

import net.gommagomma.sbam.physics.Wire;

/** La tensione di un filo (del suo nodo) [V]. */
public final class VoltageSignal extends AnalogSignal
{
    private final Wire wire;

    public VoltageSignal(Wire wire)
    {
        super("wires", wire.name());
        this.wire = wire;
    }

    public Wire wire()        { return wire; }

    @Override
    public String unit()      { return "V"; }

    @Override
    public double value()     { return wire.volts(); }
}
