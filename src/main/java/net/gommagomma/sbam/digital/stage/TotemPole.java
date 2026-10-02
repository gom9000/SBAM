package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Drive;

/** Uscita totem-pole: pilota sempre, H o L (es. 74HC14, 74HC138). */
public final class TotemPole extends PushPull
{
    public TotemPole(HighSide high, LowSide low, double ratedHigh, double ratedLow, double capacitance)
    {
        super(high, low, ratedHigh, ratedLow, capacitance);
    }

    @Override public boolean accepts(Drive drive) { return drive != Drive.Z; }
    @Override public Drive initialDrive()         { return Drive.L; }
}
