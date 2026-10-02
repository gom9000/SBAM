package net.gommagomma.sbam;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.Trace;
import net.gommagomma.sbam.instrument.digital.ContentionSentinel;
import net.gommagomma.sbam.instrument.digital.DriveSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Una simulazione con nome: i suoi file finiscono in <base>/<nome>/, e una nuova esecuzione li sovrascrive. */
class SimulationTest
{
    private static void run(Path base, long untilPs) throws Exception
    {
        Simulation sim = new Simulation("clash", 1_000, base);
        Supply vcc = sim.add(new Supply("VCC", 5.0, 0.1));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Source a = sim.add(new Source("A", Families.HC_TRISTATE).at(0, Drive.H));
        Source b = sim.add(new Source("B", Families.HC_TRISTATE).at(0, Drive.Z).at(50_000, Drive.L));
        Wire bus = new Wire("BUS", 10e-12);
        a.vdd.connect(rail);
        a.gnd.connect(Ground.of(sim.engine()));
        b.vdd.connect(rail);
        b.gnd.connect(Ground.of(sim.engine()));
        a.out.connect(bus);
        b.out.connect(bus);
        sim.add(new ContentionSentinel(sim.log()));
        sim.vcd("bus").add(new DriveSignal(a.out)).add(new DriveSignal(b.out)).add(new VoltageSignal(bus));
        Trace analog = new Trace();
        sim.add(new Recorder(analog).add(new VoltageSignal(bus)).every(10));
        sim.runUntil(untilPs);
        analog.writeCsv(sim.file("analog.csv"));
        sim.close();
    }

    @Test
    void filesGoInTheFolderOfTheSimulation() throws Exception
    {
        Path base = Files.createTempDirectory("sbam");
        run(base, 100_000);
        Path dir = base.resolve("clash");
        for (String f : new String[] { "bus.vcd", "analog.csv", "events.txt", "run.txt" }) {
            assertTrue(Files.exists(dir.resolve(f)), f);
        }
        assertTrue(Files.readString(dir.resolve("bus.vcd")).contains("$enddefinitions"));
        assertTrue(Files.readString(dir.resolve("events.txt")).contains("KABOOM"));
        String summary = Files.readString(dir.resolve("run.txt"));
        assertTrue(summary.contains("clash") && summary.contains("100 ns") && summary.contains("KABOOM! 1"), summary);
    }

    @Test
    void aNewRunOverwritesTheFiles() throws Exception
    {
        Path base = Files.createTempDirectory("sbam");
        run(base, 100_000);
        long first = Files.readAllLines(base.resolve("clash/analog.csv")).size();
        run(base, 30_000);                        // più breve, e prima dello scontro
        long second = Files.readAllLines(base.resolve("clash/analog.csv")).size();
        assertTrue(second < first, "riscritto, non accodato: " + first + " -> " + second);
        assertFalse(Files.readString(base.resolve("clash/events.txt")).contains("KABOOM"));
    }

    @Test
    void theNameMustBeAFolderName()
    {
        assertThrows(IllegalArgumentException.class, () -> new Simulation("../fuori", 1_000));
        assertThrows(IllegalArgumentException.class, () -> new Simulation("con spazio", 1_000));
        assertEquals("ok-1.2_x", new Simulation("ok-1.2_x", 1_000).name());
    }
}
