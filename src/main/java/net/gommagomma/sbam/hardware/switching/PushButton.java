package net.gommagomma.sbam.hardware.switching;

import net.gommagomma.sbam.logic.OperatedDevice;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/** Un pulsante normalmente aperto tra A e B. Lo stimolo: 1 premuto, 0 rilasciato. */
public final class PushButton extends OperatedDevice
{
    /** Resistenza del contatto chiuso [ohm] (valore tipico). */
    private final Pin a;
    private final Pin b;

    public PushButton(String name, Stimulus... stimuli)
    {
        super(name, stimuli);
        a = pin("A", 1e-12);
        b = pin("B", 1e-12);
    }

    public Pin a()  { return a; }
    public Pin b()  { return b; }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction out)
    {
        if (position() == 0) return;                               // rilasciato
        Contact.closed(a, b, trial, out);
    }
}
