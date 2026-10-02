package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.instrument.Capture;
import net.gommagomma.sbam.instrument.Signal;
import net.gommagomma.sbam.physics.Tick;

/** Una parola su una porta digitale: il valore, i bit indefiniti, i bit rilasciati. */
public abstract class BusSignal extends Signal
{
    private final DigitalPort port;

    protected BusSignal(DigitalPort port, String what)
    {
        super(port.device().name(), port.name() + "." + what);
        this.port = port;
    }

    public final DigitalPort port() { return port; }

    public abstract long value();

    /** I bit indefiniti (letti X). */
    public abstract long unknown();

    /** I bit rilasciati (Z). */
    public abstract long released();

    @Override
    public final void declare(Capture capture)
    {
        capture.declareWord(this, port.width());
    }

    @Override
    public final void sample(Tick tick, Capture capture)
    {
        capture.word(this, tick.next(), value(), unknown(), released());
    }
}
