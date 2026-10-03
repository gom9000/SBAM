package net.gommagomma.sbam.hardware.switching;

import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Un contatto chiuso tra due pin: una resistenza piccola, quella dei contatti di un interruttore.
 * Aperto non c'è nulla da dire: i pin restano scollegati. Lo usano tutti gli interruttori.
 */
final class Contact
{
    private Contact() {}

    /** La resistenza di un contatto chiuso [ohm]. */
    static final double CLOSED_OHMS = 0.05;

    /** I due pin collegati dal contatto chiuso. */
    static void closed(Pin a, Pin b, Voltages trial, Reaction out)
    {
        out.set(a, trial.volts(b), CLOSED_OHMS, 0.0);
        out.set(b, trial.volts(a), CLOSED_OHMS, 0.0);
    }
}
