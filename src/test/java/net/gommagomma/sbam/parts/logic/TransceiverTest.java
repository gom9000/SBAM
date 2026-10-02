package net.gommagomma.sbam.parts.logic;

import net.gommagomma.sbam.parts.logic.Transceiver;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSink;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Transceiver (come un 74HC245): verso, abilitazione e ritardi. */
class TransceiverTest
{
    private Engine engine;
    private Transceiver u;
    private WordSource fromA, fromB;
    private WordSink atA, atB;

    /** Una sorgente sul lato dato, un lettore sull'altro; DIR e OE# da due sorgenti con i programmi dati. */
    private void build(boolean sourceOnA, Source dir, Source oe)
    {
        engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        u = engine.add(new Transceiver("U", Families.HC, 8, 9_000, 15_000, 12_000));   // 74HC245
        u.vdd().connect(rail);
        u.gnd().connect(Ground.of(engine));
        engine.add(dir);
        engine.add(oe);
        dir.vdd.connect(rail);
        dir.gnd.connect(Ground.of(engine));
        oe.vdd.connect(rail);
        oe.gnd.connect(Ground.of(engine));
        dir.out.connect(new Wire("DIR", 5e-12));
        u.dir().connect(dir.out.wires().get(0));
        oe.out.connect(new Wire("OE", 5e-12));
        u.oe().connect(oe.out.wires().get(0));

        Bus a = Bus.of("A", 8, 10e-12), b = Bus.of("B", 8, 10e-12);
        u.a().connect(a);
        u.b().connect(b);
        WordSource src = engine.add(new WordSource("SRC", Families.HC, 8).at(0, 0xA5, 0xFF));
        WordSink dst = engine.add(new WordSink("DST", Families.HC_IN, 8));
        src.vdd.connect(rail);
        src.gnd.connect(Ground.of(engine));
        dst.vdd.connect(rail);
        dst.gnd.connect(Ground.of(engine));
        src.port.connect(sourceOnA ? a : b);
        dst.port.connect(sourceOnA ? b : a);
        if (sourceOnA) { fromA = src; atB = dst; } else { fromB = src; atA = dst; }
    }

    @Test
    void fromAToBOnlyWhenEnabledAndAfterTheEnableTime()
    {
        build(true,
              new Source("DIRSRC", Families.HC_TRISTATE).at(0, Drive.H),
              new Source("OESRC", Families.HC_TRISTATE).at(0, Drive.H).at(100_000, Drive.L).at(300_000, Drive.H));
        engine.runUntil(100_000);
        assertEquals(0xFF, u.b().released(), "OE# alto: lato B rilasciato");
        engine.runUntil(105_000);
        assertEquals(0xFF, u.b().released(), "non ancora: ten");
        engine.runUntil(150_000);
        assertEquals(0, u.b().released());
        assertEquals(0xA5, u.b().driven());
        assertEquals(0xA5, atB.port.read(), "il dato arriva dall'altra parte");
        assertEquals(0xFF, u.a().released(), "il lato A non pilota");
        engine.runUntil(350_000);
        assertEquals(0xFF, u.b().released(), "OE# di nuovo alto: rilasciato dopo tdis");
    }

    @Test
    void fromBToAWhenDirIsLow()
    {
        build(false,
              new Source("DIRSRC", Families.HC_TRISTATE).at(0, Drive.L),
              new Source("OESRC", Families.HC_TRISTATE).at(0, Drive.L));
        engine.runUntil(100_000);
        assertEquals(0xA5, atA.port.read());
        assertEquals(0xFF, u.b().released());
    }
}
