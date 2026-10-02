package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.instrument.Event;
import net.gommagomma.sbam.instrument.Severity;

import java.util.List;

/**
 * Fa avanzare una simulazione nel suo thread, a blocchi di tick, e prende i comandi tra un blocco e l'altro:
 * avvia, ferma, un tick, avanza di un tempo, velocità, fermati su un evento.
 *
 * Chi vuole leggere lo stato della simulazione (le sonde, il registro degli eventi) lo fa tenendo lock():
 * il runner lo tiene per tutto un blocco, e un blocco dura al più qualche decina di millisecondi.
 * Non sa nulla di Swing.
 */
public final class Runner implements Runnable
{
    /** Quanto dura al più un blocco di tick tenendo il lock [ns di tempo reale]. */
    private static final long BATCH_NANOS = 20_000_000L;

    private final Simulation sim;
    private final Object lock = new Object();
    private final Object wake = new Object();

    private volatile boolean running = false;
    private volatile boolean quit = false;
    private volatile long targetPs = Long.MAX_VALUE;
    private volatile long psPerSecond = 0;            // 0: la velocità massima
    private volatile Severity stopOn = null;          // null: non fermarti sugli eventi
    private volatile String status = "pronto";
    private volatile double measuredPsPerSecond = 0;

    private int seenEvents = 0;
    private long anchorWall, anchorSim;               // per la velocità: da dove si conta

    public Runner(Simulation sim)
    {
        this.sim = sim;
    }

    /** Il lock da tenere per leggere lo stato della simulazione. */
    public Object lock()               { return lock; }
    public Simulation simulation()     { return sim; }
    public boolean running()           { return running; }
    /** Perché è ferma, o che cosa sta facendo. */
    public String status()             { return status; }
    /** La velocità misurata nell'ultimo blocco [ps simulati al secondo]. */
    public double measuredSpeed()      { return measuredPsPerSecond; }

    // ------------------------------------------------------------ comandi (da qualunque thread)

    /** Avvia, senza un istante di arrivo. */
    public void start()
    {
        targetPs = Long.MAX_VALUE;
        resume("in corsa");
    }

    /** Avanza di un tempo dato e si ferma. */
    public void advance(long ps)
    {
        synchronized (lock) {
            targetPs = sim.engine().nowPs() + ps;
        }
        resume("avanza fino a " + Format.time(targetPs));
    }

    public void pause()
    {
        running = false;
        status = "in pausa";
    }

    /** Un tick, se è ferma. */
    public void step()
    {
        if (running) return;
        synchronized (lock) {
            sim.step();
            if (!checkEvents()) status = "in pausa";
        }
    }

    /** La velocità: ps simulati per secondo reale; 0 per la massima. */
    public void speed(long psPerSecond)
    {
        this.psPerSecond = psPerSecond;
        anchor();
    }

    /** Fermati al primo evento di questa gravità o più grave; null per non fermarti. */
    public void stopOn(Severity severity)
    {
        this.stopOn = severity;
    }

    /** Termina il thread (la simulazione non viene chiusa: lo fa chi l'ha creata). */
    public void quit()
    {
        quit = true;
        running = false;
        synchronized (wake) {
            wake.notifyAll();
        }
    }

    // ------------------------------------------------------------ il thread

    @Override
    public void run()
    {
        while (!quit) {
            if (!running) {
                waitForStart();
                continue;
            }
            long wall0 = System.nanoTime();
            long sim0;
            synchronized (lock) {
                sim0 = sim.engine().nowPs();
                batch(wall0);
            }
            long wall1 = System.nanoTime();
            long simulated;
            synchronized (lock) {
                simulated = sim.engine().nowPs() - sim0;
            }
            if (wall1 > wall0) measuredPsPerSecond = simulated * 1e9 / (wall1 - wall0);
            if (running && psPerSecond > 0 && ahead()) sleep(10);
        }
    }

    private void batch(long wall0)
    {
        long deadline = wall0 + BATCH_NANOS;
        do {
            if (sim.engine().nowPs() >= targetPs) {
                running = false;
                status = "arrivata a " + Format.time(sim.engine().nowPs());
                return;
            }
            if (psPerSecond > 0 && ahead()) return;
            sim.step();
            if (checkEvents()) return;
        } while (System.nanoTime() < deadline);
    }

    /** Guarda gli eventi nuovi; vero se uno di loro ha fermato la simulazione. */
    private boolean checkEvents()
    {
        List<Event> events = sim.log().events();
        boolean stopped = false;
        Severity threshold = stopOn;
        for (int i = seenEvents; i < events.size(); i++) {
            Event e = events.get(i);
            if (!stopped && threshold != null && e.severity().compareTo(threshold) >= 0) {
                running = false;
                status = "fermata: " + e.severity().sound() + " " + e.source() + " a " + Format.time(e.timePs());
                stopped = true;
            }
        }
        seenEvents = events.size();
        return stopped;
    }

    /** Vero se la simulazione è avanti rispetto alla velocità scelta. */
    private boolean ahead()
    {
        double allowed = anchorSim + (System.nanoTime() - anchorWall) * 1e-9 * psPerSecond;
        return sim.engine().nowPs() >= allowed;
    }

    private void resume(String what)
    {
        status = what;
        anchor();
        running = true;
        synchronized (wake) {
            wake.notifyAll();
        }
    }

    private void anchor()
    {
        anchorWall = System.nanoTime();
        synchronized (lock) {
            anchorSim = sim.engine().nowPs();
        }
    }

    private void waitForStart()
    {
        synchronized (wake) {
            while (!running && !quit) {
                try {
                    wake.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    quit = true;
                }
            }
        }
    }

    private static void sleep(long ms)
    {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
