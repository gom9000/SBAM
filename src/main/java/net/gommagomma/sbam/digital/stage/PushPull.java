package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Uno stadio d'uscita con le due metà: alta verso VDD, bassa verso GND.
 * Le varianti (TotemPole, TriState) differiscono solo in ciò che accettano e in come partono.
 */
public abstract class PushPull implements OutputStage
{
    private final HighSide high;
    private final LowSide low;
    private final double ratedHigh;
    private final double ratedLow;
    private final double capacitance;

    protected PushPull(HighSide high, LowSide low, double ratedHigh, double ratedLow, double capacitance)
    {
        this.high = high;
        this.low = low;
        this.ratedHigh = ratedHigh;
        this.ratedLow = ratedLow;
        this.capacitance = capacitance;
    }

    public final HighSide high()      { return high; }
    public final LowSide low()        { return low; }

    @Override
    public final double capacitance() { return capacitance; }

    @Override
    public final void react(Pin pin, Power power, Drive drive, Voltages trial, Reaction out)
    {
        if (drive == Drive.H) high.react(pin, power.vdd(), trial, out);
        if (drive == Drive.L) low.react(pin, power.gnd(), trial, out);
    }

    @Override
    public final double ratedCurrent(Drive drive)
    {
        return drive == Drive.H ? ratedHigh : drive == Drive.L ? ratedLow : 0.0;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + high + ", " + low + ", " + ratedHigh + ", " + ratedLow + ", " + capacitance + ")";
    }
}
