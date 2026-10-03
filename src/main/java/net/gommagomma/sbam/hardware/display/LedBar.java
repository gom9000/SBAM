package net.gommagomma.sbam.hardware.display;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Port;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Una barra di N LED: il LED i tra l'anodo A_i e il catodo K_i.
 * Rossi: soglia ~2 V, ~15 ohm in conduzione (valori indicativi). Un LED si vede acceso oltre 1 mA.
 */
public final class LedBar extends Device
{
    /** Corrente oltre la quale un LED si vede acceso [A]. */
    public static final double VISIBLE_AMPS = 1e-3;

    private final Port<Pin> anodes;
    private final Port<Pin> cathodes;
    private static final double FORWARD_VOLTS = 2.0;
    private static final double ON_OHMS = 15.0;

    public LedBar(String name, int segments)
    {
        super(name);
        anodes = port("A", segments, 1e-12);
        cathodes = port("K", segments, 1e-12);
    }

    public Port<Pin> a()  { return anodes; }
    public Port<Pin> k()  { return cathodes; }

    /** La corrente nel LED i, dall'anodo al catodo [A]. */
    public double current(int segment)
    {
        return -anodes.get(segment).current();
    }

    public boolean lit(int segment)
    {
        return current(segment) > VISIBLE_AMPS;
    }

    /** I LED accesi, come parola (bit i = LED i). */
    public long litMask()
    {
        long m = 0;
        for (int i = 0; i < anodes.width(); i++) {
            if (lit(i)) m |= 1L << i;
        }
        return m;
    }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction out)
    {
        for (int i = 0; i < anodes.width(); i++) {
            Pin an = anodes.get(i), k = cathodes.get(i);
            double va = trial.volts(an), vk = trial.volts(k);
            if (va - vk <= FORWARD_VOLTS) continue;                // spento
            out.set(an, vk + FORWARD_VOLTS, ON_OHMS, 0.0);
            out.set(k, va - FORWARD_VOLTS, ON_OHMS, 0.0);
        }
    }

    @Override
    protected void update(Tick tick, Voltages settled)
    {
    }
}
