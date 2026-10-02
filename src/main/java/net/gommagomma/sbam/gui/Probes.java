package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.Signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le sonde della finestra: i Signal di sempre, ognuno con il nome da mostrare. Le logiche e le parole vanno
 * nell'analizzatore logico, le analogiche nell'oscilloscopio, nell'ordine in cui si aggiungono.
 */
public final class Probes
{
    private final ChangeTrace trace = new ChangeTrace();
    private final Recorder recorder = new Recorder(trace);
    private final List<String> labels = new ArrayList<>();

    Probes() {}

    /** Una sonda con il suo nome sullo schermo ("clock", "bus indirizzi"). */
    public Probes probe(Signal signal, String label)
    {
        recorder.add(signal);
        labels.add(label);
        return this;
    }

    /** Una sonda con il nome del segnale. */
    public Probes probe(Signal signal)
    {
        String name = signal.fullName();
        return probe(signal, name.startsWith("wires.") ? name.substring("wires.".length()) : name);
    }

    ChangeTrace trace()        { return trace; }
    Recorder recorder()        { return recorder; }
    List<String> labels()      { return Collections.unmodifiableList(labels); }
}
