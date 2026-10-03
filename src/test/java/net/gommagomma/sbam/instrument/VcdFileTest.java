package net.gommagomma.sbam.instrument;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Sink;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSink;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.digital.DriveSignal;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.instrument.digital.PortReadSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Il VCD: intestazione dai segnali dichiarati, poi solo i cambiamenti. */
class VcdFileTest
{
    private static Wire rail(Engine engine)
    {
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        return rail;
    }

    @Test
    void writesHeaderAndOnlyTheChanges()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        Source src = engine.add(new Source("SRC", Families.HC_TRISTATE)
                .at(0, Drive.L).at(100_000, Drive.H).at(300_000, Drive.Z));
        Sink in = engine.add(new Sink("IN", Families.HC_IN));
        Wire a = new Wire("A", 10e-12);
        src.vdd.connect(rail);
        src.gnd.connect(Ground.of(engine));
        in.vdd.connect(rail);
        in.gnd.connect(Ground.of(engine));
        src.out.connect(a);
        in.in.connect(a);

        StringWriter text = new StringWriter();
        Recorder vcd = engine.add(new Recorder(new VcdFile(text).resolution("V", 0.5))
                .add(new DriveSignal(src.out)).add(new LevelSignal(in.in)).add(new VoltageSignal(a)));
        engine.runUntil(1_000_000);
        vcd.close();
        assertThrows(IllegalStateException.class, () -> vcd.add(new DriveSignal(src.out)), "configurazione chiusa una volta avviato");

        String s = text.toString();
        List<String> lines = Arrays.asList(s.split("\n"));
        assertTrue(s.contains("$timescale 1ps $end"));
        assertTrue(s.contains("$scope module SRC $end\n$var wire 1 ! Y_drive $end"), s);
        assertTrue(s.contains("$var wire 1 \" A_level $end"), s);
        assertTrue(s.contains("$scope module wires $end\n$var real 64 # A $end"), s);
        assertTrue(lines.contains("#100000") && lines.contains("1!"), "l'intenzione sale a 100 ns");
        assertTrue(lines.contains("#300000") && lines.contains("z!"), "rilascio a 300 ns");
        assertTrue(lines.contains("x\""), "il livello letto attraversa x durante il fronte");
        int stamps = 0;
        for (String l : lines) {
            if (l.startsWith("#")) stamps++;
        }
        assertTrue(stamps < 30, "solo gli istanti con cambiamenti: " + stamps + " su 1000 tick");
    }

    @Test
    void wordsAreBitVectorsWithXAndZ()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        WordSource cpu = engine.add(new WordSource("CPU", Families.HC, 4).at(0, 0b0101, 0b0111));
        WordSink ram = engine.add(new WordSink("RAM", Families.HC_IN, 4));
        cpu.vdd.connect(rail);
        cpu.gnd.connect(Ground.of(engine));
        ram.vdd.connect(rail);
        ram.gnd.connect(Ground.of(engine));
        Bus d = Bus.of("D", 4, 10e-12);
        cpu.port.connect(d);
        ram.port.connect(d);

        StringWriter text = new StringWriter();
        Recorder vcd = engine.add(new Recorder(new VcdFile(text))
                .add(new PortDriveSignal(cpu.port)).add(new PortReadSignal(ram.port)));
        engine.runUntil(50_000);
        vcd.close();

        String s = text.toString();
        assertTrue(s.contains("$var wire 4 ! D_drive $end"), s);
        assertTrue(s.contains("$var wire 4 \" D_read $end"), s);
        assertTrue(s.contains("bz101 !"), "il bit 3 è rilasciato: " + s);
        assertTrue(s.contains("b0101 \""), "il bit 3 flottante resta a 0 dalla carica iniziale: " + s);
    }
}
