package net.gommagomma.sbam.instrument;

import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;
import net.gommagomma.sbam.physics.Tick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lo strumento che registra: a ogni campionamento chiede ai suoi segnali di consegnare il valore
 * alla sua capture. Si configura prima di avviare, si chiude alla fine.
 *
 *     Trace t = new Trace();
 *     engine.add(new Recorder(t).add(new VoltageSignal(vc)).add(new CurrentSignal(r.b())).every(10));
 */
public final class Recorder implements Instrument
{
    private final Capture capture;
    private final List<Signal> signals = new ArrayList<>();
    private int every = 1;
    private long ticks = 0;
    private boolean started = false;
    private boolean ended = false;

    public Recorder(Capture capture)
    {
        this.capture = capture;
    }

    public Recorder add(Signal signal)
    {
        requireNotStarted();
        signals.add(signal);
        return this;
    }

    /** Un campione ogni n tick (default 1). */
    public Recorder every(int n)
    {
        requireNotStarted();
        if (n < 1) throw new IllegalArgumentException("ogni quanti tick: almeno 1");
        this.every = n;
        return this;
    }

    public List<Signal> signals() { return Collections.unmodifiableList(signals); }

    @Override
    public void observe(Tick tick, Engine engine)
    {
        if (ended) return;                     // chiusa: non registra più
        if (!started) start();
        if (ticks++ % every != 0) return;
        for (Signal s : signals) s.sample(tick, capture);
    }

    /** Fine della registrazione: la capture viene chiusa. Chiamarlo più volte non fa nulla. */
    public void close()
    {
        if (ended) return;
        if (!started) start();
        ended = true;
        capture.end();
    }

    private void start()
    {
        started = true;
        for (Signal s : signals) s.declare(capture);
        capture.begin();
    }

    private void requireNotStarted()
    {
        if (started) throw new IllegalStateException("registrazione già avviata: si configura prima di avviare");
    }
}
