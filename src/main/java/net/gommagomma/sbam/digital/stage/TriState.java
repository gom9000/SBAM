package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Drive;

/** Uscita tri-state: H, L oppure alta impedenza (es. 74HC244, RAM, porte del PIC). */
public final class TriState extends PushPull
{
    public TriState(HighSide high, LowSide low, double ratedHigh, double ratedLow, double capacitance)
    {
        super(high, low, ratedHigh, ratedLow, capacitance);
    }

    @Override public boolean accepts(Drive drive) { return true; }
    @Override public Drive initialDrive()         { return Drive.Z; }
}
