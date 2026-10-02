package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Il runner, senza finestra: avanza di un tempo, si ferma su un evento, fa un tick alla volta. */
class RunnerTest
{
    /** Uno strumento che a un istante dato scrive un evento. */
    private static final class Alarm implements Instrument
    {
        private final EventLog log;
        private final long atPs;
        private final Severity severity;

        Alarm(EventLog log, long atPs, Severity severity)
        {
            this.log = log;
            this.atPs = atPs;
            this.severity = severity;
        }

        @Override
        public void observe(Tick tick, Engine engine)
        {
            if (tick.nowPs() == atPs) log.add(tick.nowPs(), severity, "ALARM", "eccolo");
        }
    }

    private static Simulation simulation(Path base)
    {
        Simulation sim = new Simulation("runner", 1_000, base);
        Supply vcc = sim.add(new Supply("VCC", 5.0, 0.1));
        vcc.out().connect(new Wire("+5V", 100e-12));
        sim.add(new Alarm(sim.log(), 50_000, Severity.BZZT));
        sim.add(new Alarm(sim.log(), 80_000, Severity.KABOOM));
        return sim;
    }

    private static long now(Runner r)
    {
        synchronized (r.lock()) {
            return r.simulation().engine().nowPs();
        }
    }

    private static void waitStopped(Runner r) throws InterruptedException
    {
        for (int i = 0; i < 500 && r.running(); i++) Thread.sleep(10);
        assertFalse(r.running(), "il runner doveva fermarsi");
    }

    @Test
    void advanceStopsAtTheGivenTime() throws Exception
    {
        Path base = Files.createTempDirectory("sbam");
        Runner r = new Runner(simulation(base));
        Thread t = new Thread(r);
        t.start();
        r.advance(20_000);
        waitStopped(r);
        assertEquals(20_000, now(r));
        r.step();
        assertEquals(21_000, now(r));
        r.quit();
        t.join(2_000);
    }

    @Test
    void itStopsOnTheFirstEventSevereEnough() throws Exception
    {
        Path base = Files.createTempDirectory("sbam");
        Runner r = new Runner(simulation(base));
        r.stopOn(Severity.CRASH);                       // il BZZT a 50 ns non basta, il KABOOM a 80 ns sì
        Thread t = new Thread(r);
        t.start();
        r.start();
        waitStopped(r);
        assertEquals(81_000, now(r));                   // fermo alla fine del tick dell'evento
        assertTrue(r.status().startsWith("fermata: KABOOM"), r.status());
        r.quit();
        t.join(2_000);
    }

    @Test
    void timesAreWrittenAndReadWithTheirUnit()
    {
        assertEquals("250 ns", Format.time(250_000));
        assertEquals("12,5 µs", Format.time(12_500_000));
        assertEquals("1 ps", Format.time(1));
        assertEquals(10_000_000, Format.parseTime("10us"));
        assertEquals(1_500_000_000L, Format.parseTime("1,5 ms"));
        assertEquals(250_000, Format.parseTime("250 ns"));
        assertEquals("3A", Format.word(0x3A, 0, 0, 8));
        assertEquals("XA", Format.word(0x0A, 0x10, 0, 8));
        assertEquals("ZZ", Format.word(0, 0, 0xFF, 8));
        assertEquals("0C00", Format.word(0xC00, 0, 0, 15));
    }
}
