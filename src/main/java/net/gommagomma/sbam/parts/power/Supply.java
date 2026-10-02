package net.gommagomma.sbam.parts.power;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/** Alimentatore ideale con resistenza interna: un generatore di tensione su un pin. */
public final class Supply extends Device
{
    private final Pin out;
    private final double volts;
    private final double ohms;

    public Supply(String name, double volts, double ohms)
    {
        super(name);
        this.volts = volts;
        this.ohms = ohms;
        this.out = pin("OUT", 0.0);
    }

    public Pin out() { return out; }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction r)
    {
        r.set(out, volts, ohms, 0.0);
    }

    @Override
    protected void update(Tick tick, Voltages settled)
    {
    }
}
