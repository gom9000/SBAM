package net.gommagomma.sbam.hardware.logic;

import net.gommagomma.sbam.hardware.logic.Buffer;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Buffer: due metà di un 74HC244, ciascuna con il suo OE#. */
class BufferTest
{
    @Test
    void eachHalfFollowsItsOwnEnable()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Wire ground = Ground.of(engine);
        Buffer low = engine.add(new Buffer("U1A", Families.HC, 4, 9_000, 14_000, 12_000));   // 74HC244
        Buffer high = engine.add(new Buffer("U1B", Families.HC, 4, 9_000, 14_000, 12_000));   // 74HC244
        for (Buffer b : new Buffer[] { low, high }) {
            b.vdd().connect(rail);
            b.gnd().connect(ground);
        }
        WordSource src = engine.add(new WordSource("SRC", Families.HC, 8).at(0, 0x96, 0xFF));
        src.vdd.connect(rail);
        src.gnd.connect(ground);
        Bus a = Bus.of("A", 8, 5e-12);
        src.port.connect(a);
        Bus y = Bus.of("Y", 8, 5e-12);
        low.a().connect(a.slice(0, 4));
        high.a().connect(a.slice(4, 8));
        low.y().connect(y.slice(0, 4));
        high.y().connect(y.slice(4, 8));
        Source oe1 = engine.add(new Source("S1", Families.HC_TRISTATE).at(0, Drive.L));
        Source oe2 = engine.add(new Source("S2", Families.HC_TRISTATE).at(0, Drive.H).at(200_000, Drive.L));
        for (Source s : new Source[] { oe1, oe2 }) {
            s.vdd.connect(rail);
            s.gnd.connect(ground);
        }
        Wire w1 = new Wire("OE1", 5e-12), w2 = new Wire("OE2", 5e-12);
        oe1.out.connect(w1);
        low.oe().connect(w1);
        oe2.out.connect(w2);
        high.oe().connect(w2);

        engine.runUntil(100_000);
        assertEquals(0xF, high.y().released(), "solo la metà bassa abilitata");
        assertEquals(0, low.y().released());
        assertEquals(0x6, low.y().driven());
        engine.runUntil(300_000);
        assertEquals(0, high.y().released());
        assertEquals(0x9, high.y().driven());
    }
}
