package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Level;

/**
 * Ingresso a trigger di Schmitt: le soglie sono VT+ (salita) e VT- (discesa).
 * Tra le due conserva il livello letto prima; senza storia, sceglie il lato più vicino.
 * Non legge mai X.
 */
public final class SchmittInput extends ThresholdInput
{
    public SchmittInput(Thresholds thresholds, Loading loading)
    {
        super(thresholds, loading);
    }

    @Override
    public Level read(double volts, double vdd, Level previous)
    {
        double hi = thresholds().high(vdd), lo = thresholds().low(vdd);
        if (volts >= hi) return Level.H;
        if (volts <= lo) return Level.L;
        if (previous != Level.X) return previous;
        return volts >= (hi + lo) / 2 ? Level.H : Level.L;
    }
}
