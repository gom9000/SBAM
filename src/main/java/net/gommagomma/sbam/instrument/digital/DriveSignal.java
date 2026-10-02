package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPin;

/** L'intenzione dell'uscita di un pin: L, H o Z. */
public final class DriveSignal extends LogicSignal
{
    public DriveSignal(DigitalPin pin)
    {
        super(pin, "drive");
    }

    @Override
    public char value() { return pin().driven().name().charAt(0); }
}
