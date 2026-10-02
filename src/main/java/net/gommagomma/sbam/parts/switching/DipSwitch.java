package net.gommagomma.sbam.parts.switching;

import net.gommagomma.sbam.logic.OperatedDevice;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Port;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Un dip switch a N vie: l'interruttore i collega il pin A_i al pin B_i.
 * Lo stimolo è la posizione delle levette: il bit i a 1 vuol dire interruttore i chiuso (ON).
 * Chiuso è una resistenza di contatto piccola, aperto non conduce.
 */
public final class DipSwitch extends OperatedDevice
{
    /** Resistenza di un contatto chiuso [ohm] (valore tipico). */
    public static final double CLOSED_OHMS = 0.05;

    private final Port<Pin> a;
    private final Port<Pin> b;

    public DipSwitch(String name, int ways, Stimulus... stimuli)
    {
        super(name, stimuli);
        a = port("A", ways, 1e-12);
        b = port("B", ways, 1e-12);
    }

    public Port<Pin> a()  { return a; }
    public Port<Pin> b()  { return b; }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction out)
    {
        for (int i = 0; i < a.width(); i++) {
            if ((position() & (1L << i)) == 0) continue;          // aperto
            out.set(a.get(i), trial.volts(b.get(i)), CLOSED_OHMS, 0.0);
            out.set(b.get(i), trial.volts(a.get(i)), CLOSED_OHMS, 0.0);
        }
    }
}
