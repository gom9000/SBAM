package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.hardware.cpu.Cpu;
import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.Signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le sonde della finestra: i Signal di sempre, ognuno con il nome da mostrare. Le logiche e le parole vanno
 * nell'analizzatore logico, le analogiche nell'oscilloscopio, nell'ordine in cui si aggiungono. Le CPU da seguire
 * hanno un pannello loro.
 */
public final class Probes
{
    private final ChangeTrace trace = new ChangeTrace();
    private final Recorder recorder = new Recorder(trace);
    private final List<String> labels = new ArrayList<>();
    private final List<CpuWatch> cpus = new ArrayList<>();

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

    /** Una CPU da seguire: i registri, l'istruzione in corso, e quelle eseguite. */
    public Probes cpu(Cpu cpu, String label)
    {
        cpus.add(new CpuWatch(cpu, label));
        return this;
    }

    ChangeTrace trace()        { return trace; }
    List<CpuWatch> cpus()      { return Collections.unmodifiableList(cpus); }
    Recorder recorder()        { return recorder; }
    List<String> labels()      { return Collections.unmodifiableList(labels); }
}
