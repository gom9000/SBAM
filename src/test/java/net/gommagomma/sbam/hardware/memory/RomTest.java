package net.gommagomma.sbam.hardware.memory;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import net.gommagomma.sbam.program.image.MemoryImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La ROM: il contenuto programmato, i tempi, le celle vuote; e la RAM che carica la stessa immagine. */
class RomTest
{
    @Test
    void readsItsContentAfterTheAccessTimeAndFFWhereNothingWasWritten()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.01));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Rom rom = engine.add(new Rom("U", Families.HC, 15, 8, 150_000, 75_000, 50_000, new MemoryImage().write(0x1234, 0xA5)));
        rom.vdd().connect(rail);
        rom.gnd().connect(Ground.of(engine));
        WordSource address = engine.add(new WordSource("ADDR", Families.HC, 15).at(0, 0x1234, 0x7FFF).at(600_000, 0x1235, 0x7FFF));
        Source ce = engine.add(new Source("CE", Families.HC_TRISTATE).at(0, Drive.L));
        Source oe = engine.add(new Source("OE", Families.HC_TRISTATE).at(0, Drive.H).at(400_000, Drive.L));
        address.vdd.connect(rail); address.gnd.connect(Ground.of(engine));
        for (Source s : new Source[] { ce, oe }) { s.vdd.connect(rail); s.gnd.connect(Ground.of(engine)); }
        Bus a = Bus.of("A", 15, 10e-12);
        address.port.connect(a);
        rom.address().connect(a);
        rom.data().connect(Bus.of("D", 8, 10e-12));
        Wire wce = new Wire("CE", 5e-12), woe = new Wire("OE", 5e-12);
        ce.out.connect(wce);  rom.ce().connect(wce);
        oe.out.connect(woe);  rom.oe().connect(woe);
        ChangeTrace out = new ChangeTrace();
        engine.add(new Recorder(out).add(new PortDriveSignal(rom.data())));
        engine.runUntil(1_000_000);

        int first = -1, second = -1;
        for (int i = 0; i < out.size(); i++) {
            if (first < 0 && out.released(i) == 0) first = i;
            if (out.word(i) == 0xFF && out.released(i) == 0) second = i;
        }
        assertTrue(first > 0, "la ROM pilota i dati");
        assertEquals(0xA5L, out.word(first));
        long sinceOe = out.time(first) - 400_000;
        assertTrue(sinceOe >= 75_000 && sinceOe <= 78_000, "conta tOE: " + sinceOe);
        assertTrue(second > first, "la cella vuota vale FF");
        long sinceAddress = out.time(second) - 600_000;
        assertTrue(sinceAddress >= 150_000 && sinceAddress <= 160_000, "conta tACC: " + sinceAddress);
        assertEquals(0xFFL, rom.peek(0x0000));
    }

    @Test
    void anImageThatDoesNotFitIsRefused()
    {
        MemoryImage big = new MemoryImage().write(0x8000, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new Rom("U", Families.HC, 15, 8, 150_000, 75_000, 50_000, big));
    }

    @Test
    void theRamLoadsTheSameImage()
    {
        Memory ram = new Memory("U", Families.HCT, 15, 8, 70_000, 35_000, 25_000).load(new MemoryImage().write(0x10, 1, 2, 3));
        assertEquals(2L, ram.peek(0x11));
        assertEquals(0L, ram.peek(0x13));
    }
}
