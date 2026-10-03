package net.gommagomma.sbam.instrument.physics;

import net.gommagomma.sbam.fixtures.Fixtures.Switch;
import net.gommagomma.sbam.fixtures.Recorders.Meddler;
import net.gommagomma.sbam.instrument.Event;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentTest
{
    @Test
    void currentSentinelReportsStartAndEnd()
    {
        Engine engine = new Engine(1_000);
        Switch sw = engine.add(new Switch("SW", 5.0, 100.0, 200_000));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.1));
        Wire w = new Wire("W", 10e-12);
        sw.out.connect(w);
        gnd.out().connect(w);

        EventLog log = new EventLog();
        engine.add(new CurrentSentinel(log, 20e-3));
        engine.runUntil(500_000);

        List<Event> events = log.events();
        assertEquals(4, events.size(), "inizio e fine, su entrambi i pin");
        assertEquals(1_000L, events.get(0).timePs());
        assertEquals(201_000L, events.get(2).timePs());
        assertTrue(events.get(2).message().contains("200 ns"), events.get(2).message());
        assertEquals(4, log.count(Severity.BZZT));
    }

    @Test
    void instrumentCannotAdvanceTheSimulation()
    {
        Engine engine = new Engine(1_000);
        Supply s = engine.add(new Supply("S", 5.0, 1.0));
        s.out().connect(new Wire("W", 1e-12));
        engine.add(new Meddler());
        IllegalStateException ex = assertThrows(IllegalStateException.class, engine::step);
        assertTrue(ex.getMessage().contains("osserva e basta"), ex.getMessage());
    }

    @Test
    void eventLogFindsTheWorst()
    {
        EventLog log = new EventLog();
        log.add(1, Severity.BZZT, "A", "a");
        log.add(2, Severity.KABOOM, "B", "b");
        log.add(3, Severity.CRASH, "C", "c");
        assertEquals("B", log.worst().source());
    }
}
