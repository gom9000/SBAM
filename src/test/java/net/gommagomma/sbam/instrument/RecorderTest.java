package net.gommagomma.sbam.instrument;

import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.parts.passive.Resistor;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Il registratore: chiede ai segnali di consegnare i valori alla sua capture. */
class RecorderTest
{
    @Test
    void samplesEveryNTicksIntoATrace() throws Exception
    {
        Engine engine = new Engine(10_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Resistor r = engine.add(new Resistor("R1", 1_000));
        Wire rail = new Wire("+5V", 10e-12);
        Wire vc = new Wire("VC", 1e-9);
        vcc.out().connect(rail);
        r.a().connect(rail);
        r.b().connect(vc);

        Trace t = new Trace();
        engine.add(new Recorder(t).add(new VoltageSignal(vc)).add(new CurrentSignal(r.b())).every(10));
        engine.runUntil(5_000_000);   // 500 tick

        assertEquals(Arrays.asList("wires.VC", "R1.B.current"), t.columns());
        assertEquals(50, t.size());
        assertEquals(10_000L, t.time(0), "il primo campione è alla fine del primo tick");
        int last = t.size() - 1;
        assertEquals((5.0 - t.value(0, last)) / 1_000, t.value(1, last), 1e-6, "I = (5 V - Vc) / R");

        Path csv = Files.createTempFile("sbam", ".csv");
        t.writeCsv(csv);
        List<String> lines = Files.readAllLines(csv);
        assertEquals(51, lines.size());
        assertEquals("t_ns,wires.VC,R1.B.current", lines.get(0));
        Files.delete(csv);
    }

    @Test
    void aRecorderCannotBeReconfiguredOnceStarted()
    {
        Recorder r = new Recorder(new Trace());
        r.close();
        assertThrows(IllegalStateException.class, () -> r.every(5));
    }

    @Test
    void aLongTraceGrowsWithoutLosingSamples()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Wire rail = new Wire("+5V", 10e-12);
        vcc.out().connect(rail);
        Trace t = new Trace();
        engine.add(new Recorder(t).add(new VoltageSignal(rail)));
        engine.runUntil(5_000_000);   // 5000 campioni, oltre la capacità iniziale
        assertEquals(5_000, t.size());
        assertEquals(5_000_000L, t.time(4_999));
        assertEquals(5.0, t.value(0, 4_999), 1e-3);
    }
}
