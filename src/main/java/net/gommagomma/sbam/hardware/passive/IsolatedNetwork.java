package net.gommagomma.sbam.hardware.passive;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Port;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/** Una rete di N resistenze uguali e indipendenti (DIP): la resistenza i tra A_i e B_i. Tipica in serie ai LED. */
public final class IsolatedNetwork extends Device
{
    private final Port<Pin> a;
    private final Port<Pin> b;
    private final double ohms;

    public IsolatedNetwork(String name, int resistors, double ohms)
    {
        super(name);
        a = port("A", resistors, 1e-12);
        b = port("B", resistors, 1e-12);
        this.ohms = ohms;
    }

    public Port<Pin> a()  { return a; }
    public Port<Pin> b()  { return b; }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction out)
    {
        for (int i = 0; i < a.width(); i++) {
            out.set(a.get(i), trial.volts(b.get(i)), ohms, 0.0);
            out.set(b.get(i), trial.volts(a.get(i)), ohms, 0.0);
        }
    }

    @Override
    protected void update(Tick tick, Voltages settled)
    {
    }
}
