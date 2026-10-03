package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.instrument.ChangeTrace;

import java.util.List;

/**
 * Una sessione della finestra: una simulazione costruita dal setup, con le sue sonde, il suo runner e il suo
 * thread. Nasce all'apertura e a ogni reset, muore alla chiusura e a ogni reset; i pannelli la guardano.
 */
final class Session
{
    private final Simulation sim;
    private final Probes probes = new Probes();
    private final Runner runner;
    private final Thread thread;

    /** Costruisce il circuito da capo; il runner resta fermo finché non lo si avvia. */
    Session(Setup setup)
    {
        sim = setup.build(probes);
        sim.record(probes.recorder());
        for (CpuWatch w : probes.cpus()) sim.add(w);
        sim.step();                              // il primo tick: le sonde si dichiarano e prendono il primo valore
        runner = new Runner(sim);
        thread = new Thread(runner, "sbam-" + sim.name());
        thread.start();
    }

    Simulation simulation()     { return sim; }
    Runner runner()             { return runner; }
    ChangeTrace trace()         { return probes.trace(); }
    List<String> labels()       { return probes.labels(); }
    List<CpuWatch> cpus()       { return probes.cpus(); }

    /** L'istante attuale della simulazione, letto sotto il lock. */
    long nowPs()
    {
        synchronized (runner.lock()) {
            return sim.engine().nowPs();
        }
    }

    /** Ferma il runner e chiude la simulazione (i suoi file vengono scritti). */
    void close()
    {
        runner.quit();
        try {
            thread.join(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (runner.lock()) {
            sim.close();
        }
    }
}
