package net.gommagomma.sbam.parts.memory;

import net.gommagomma.sbam.parts.memory.Memory;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Memoria (come una 62256): scrittura sul fronte di WE#, lettura dopo tAA, rilascio dopo tHZ. */
class MemoryTest
{
    @Test
    void writesOnWeRisingAndReadsAfterTheAccessTime()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.01));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Memory ram = engine.add(new Memory("U", Families.HCT, 15, 8, 70_000, 35_000, 25_000));   // 62256-70
        ram.vdd().connect(rail);
        ram.gnd().connect(Ground.of(engine));

        // indirizzo 0x1234 per tutta la prova; dato 0x5A dal tester solo durante la scrittura
        WordSource address = engine.add(new WordSource("ADDR", Families.HC, 15).at(0, 0x1234, 0x7FFF));
        WordSource data = engine.add(new WordSource("DATA", Families.HC, 8).at(0, 0x5A, 0xFF).at(300_000, 0, 0));
        Source ce = engine.add(new Source("CE", Families.HC_TRISTATE).at(0, Drive.L));
        Source we = engine.add(new Source("WE", Families.HC_TRISTATE).at(0, Drive.H).at(100_000, Drive.L).at(200_000, Drive.H));
        Source oe = engine.add(new Source("OE", Families.HC_TRISTATE).at(0, Drive.H).at(400_000, Drive.L).at(700_000, Drive.H));
        for (WordSource w : new WordSource[] { address, data }) { w.vdd.connect(rail); w.gnd.connect(Ground.of(engine)); }
        for (Source s : new Source[] { ce, we, oe }) { s.vdd.connect(rail); s.gnd.connect(Ground.of(engine)); }
        Bus a = Bus.of("A", 15, 10e-12), d = Bus.of("D", 8, 10e-12);
        address.port.connect(a);
        ram.address().connect(a);
        data.port.connect(d);
        ram.data().connect(d);
        Wire wce = new Wire("CE", 5e-12), wwe = new Wire("WE", 5e-12), woe = new Wire("OE", 5e-12);
        ce.out.connect(wce);  ram.ce().connect(wce);
        we.out.connect(wwe);  ram.we().connect(wwe);
        oe.out.connect(woe);  ram.oe().connect(woe);

        ChangeTrace out = new ChangeTrace();
        engine.add(new Recorder(out).add(new PortDriveSignal(ram.data())));
        engine.runUntil(1_000_000);

        assertEquals(0x5AL, ram.peek(0x1234), "memorizzato sul fronte di salita di WE#");
        int on = -1, off = -1;
        for (int i = 0; i < out.size(); i++) {
            if (on < 0 && out.released(i) == 0) on = i;
            if (on >= 0 && off < 0 && i > on && out.released(i) == 0xFF) off = i;
        }
        assertTrue(on > 0 && off > on, "la RAM pilota e poi rilascia");
        assertEquals(0x5AL, out.word(on));
        long sinceOe = out.time(on) - 400_000;
        assertTrue(sinceOe >= 35_000 && sinceOe <= 35_000 + 3_000,
                "l'indirizzo è fermo da tempo: conta tOE (" + sinceOe + " ps)");
        long release = out.time(off) - 700_000;
        assertTrue(release >= 25_000 && release <= 25_000 + 3_000, "rilascio dopo tHZ (" + release + " ps)");
    }
}
