package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.instrument.Capture;
import net.gommagomma.sbam.instrument.Signal;
import net.gommagomma.sbam.physics.Tick;

/** Un segnale logico su un pin digitale: un carattere tra L, H, X, Z. */
public abstract class LogicSignal extends Signal
{
    private final DigitalPin pin;

    protected LogicSignal(DigitalPin pin, String what)
    {
        super(pin.device().name(), pin.name() + "." + what);
        this.pin = pin;
    }

    public final DigitalPin pin() { return pin; }

    /** Il valore alla fine dell'ultimo tick. */
    public abstract char value();

    @Override
    public final void declare(Capture capture)
    {
        capture.declareLogic(this);
    }

    @Override
    public final void sample(Tick tick, Capture capture)
    {
        capture.logic(this, tick.next(), value());
    }
}
