package net.gommagomma.sbam.parts.passive;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Resistenza tra due pin: visto da ciascun pin, è un generatore pari alla tensione
 * dell'altro pin, con in serie la resistenza.
 */
public final class Resistor extends Device
{
    private final Pin a;
    private final Pin b;
    private final double ohms;

    public Resistor(String name, double ohms)
    {
        super(name);
        this.ohms = ohms;
        this.a = pin("A", 0.0);
        this.b = pin("B", 0.0);
    }

    public Pin a() { return a; }
    public Pin b() { return b; }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction r)
    {
        r.set(a, trial.volts(b), ohms, 0.0);
        r.set(b, trial.volts(a), ohms, 0.0);
    }

    @Override
    protected void update(Tick tick, Voltages settled)
    {
    }
}
