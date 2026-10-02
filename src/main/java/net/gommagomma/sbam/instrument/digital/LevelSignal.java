package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPin;

/** Il livello letto da un pin, con le sue soglie: L, H o X. */
public final class LevelSignal extends LogicSignal
{
    public LevelSignal(DigitalPin pin)
    {
        super(pin, "level");
    }

    @Override
    public char value() { return pin().level().name().charAt(0); }
}
