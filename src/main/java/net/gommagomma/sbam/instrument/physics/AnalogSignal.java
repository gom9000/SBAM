package net.gommagomma.sbam.instrument.physics;

import net.gommagomma.sbam.instrument.Capture;
import net.gommagomma.sbam.instrument.Signal;
import net.gommagomma.sbam.physics.Tick;

/** Un segnale analogico: un numero reale con la sua unità. */
public abstract class AnalogSignal extends Signal
{
    protected AnalogSignal(String group, String name)
    {
        super(group, name);
    }

    /** L'unità di misura ("V", "A"). */
    public abstract String unit();

    /** Il valore alla fine dell'ultimo tick. */
    public abstract double value();

    @Override
    public final void declare(Capture capture)
    {
        capture.declareAnalog(this, unit());
    }

    @Override
    public final void sample(Tick tick, Capture capture)
    {
        capture.analog(this, tick.next(), value());
    }
}
