package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/** Uscita open-drain: tira a L o rilascia, mai H; serve un pull-up (es. RA4 del PIC, linee /IRQ condivise). */
public final class OpenDrain implements OutputStage
{
    private final LowSide low;
    private final double ratedLow;
    private final double capacitance;

    public OpenDrain(LowSide low, double ratedLow, double capacitance)
    {
        this.low = low;
        this.ratedLow = ratedLow;
        this.capacitance = capacitance;
    }

    public LowSide low()        { return low; }
    public double ratedLow()    { return ratedLow; }
    public double capacitance() { return capacitance; }

    @Override
    public String toString() { return "OpenDrain(" + low + ", " + ratedLow + ", " + capacitance + ")"; }

    @Override public boolean accepts(Drive drive) { return drive != Drive.H; }
    @Override public Drive initialDrive()         { return Drive.Z; }

    @Override
    public void react(Pin pin, Power power, Drive drive, Voltages trial, Reaction out)
    {
        if (drive == Drive.L) low.react(pin, power.gnd(), trial, out);
    }

    @Override
    public double ratedCurrent(Drive drive)
    {
        return drive == Drive.L ? ratedLow : 0.0;
    }
}
