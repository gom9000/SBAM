package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Level;

/** Ingresso semplice: sopra la soglia alta H, sotto la bassa L, in mezzo X. */
public final class BufferInput extends ThresholdInput
{
    public BufferInput(Thresholds thresholds, Loading loading)
    {
        super(thresholds, loading);
    }

    @Override
    public Level read(double volts, double vdd, Level previous)
    {
        if (volts >= thresholds().high(vdd)) return Level.H;
        if (volts <= thresholds().low(vdd))  return Level.L;
        return Level.X;
    }
}
