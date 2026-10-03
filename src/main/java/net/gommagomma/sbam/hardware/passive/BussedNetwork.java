package net.gommagomma.sbam.hardware.passive;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Port;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/** Una rete di N resistenze uguali con un capo comune (SIP a bus): la resistenza i tra R_i e COM. Tipica per i pull-up. */
public final class BussedNetwork extends Device
{
    private final Port<Pin> r;
    private final Pin common;
    private final double ohms;

    public BussedNetwork(String name, int resistors, double ohms)
    {
        super(name);
        r = port("R", resistors, 1e-12);
        common = pin("COM", 1e-12);
        this.ohms = ohms;
    }

    public Port<Pin> r()   { return r; }
    public Pin common()    { return common; }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction out)
    {
        for (int i = 0; i < r.width(); i++) {
            out.set(r.get(i), trial.volts(common), ohms, 0.0);
            out.set(common, trial.volts(r.get(i)), ohms, 0.0);
        }
    }

    @Override
    protected void update(Tick tick, Voltages settled)
    {
    }
}
