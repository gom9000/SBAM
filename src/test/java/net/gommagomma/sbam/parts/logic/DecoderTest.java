package net.gommagomma.sbam.parts.logic;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Il decodificatore generico, configurato come un 74HC138: tre ingressi, un enable alto e due bassi. */
class DecoderTest
{
    @Test
    void likeA138OnlyTheSelectedOutputGoesLowWhenEnabled()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Wire ground = Ground.of(engine);
        Decoder u = engine.add(new Decoder("U", Families.HC_GATE, 3, 2, 1, 10_000));
        u.vdd().connect(rail);
        u.gnd().connect(ground);
        u.y().connect(Bus.of("Y", 8, 5e-12));
        WordSource sel = engine.add(new WordSource("SEL", Families.HC, 3).at(0, 5, 7).at(300_000, 2, 7));
        sel.vdd.connect(rail);
        sel.gnd.connect(ground);
        Bus a = Bus.of("A", 3, 5e-12);
        sel.port.connect(a);
        u.select().connect(a);
        Source g2 = engine.add(new Source("G2", Families.HC_TRISTATE).at(0, Drive.H).at(100_000, Drive.L));
        g2.vdd.connect(rail);
        g2.gnd.connect(ground);
        Wire wg2 = new Wire("G2", 5e-12);
        g2.out.connect(wg2);
        u.enableLow(0).connect(wg2);
        u.enableLow(1).connect(ground);
        u.enableHigh(0).connect(rail);

        engine.runUntil(90_000);
        assertEquals(0xFF, u.y().driven(), "G2A# alto: disabilitato, tutte alte");
        engine.runUntil(200_000);
        assertEquals(0xFF & ~(1 << 5), u.y().driven(), "abilitato: Y5 bassa");
        engine.runUntil(400_000);
        assertEquals(0xFF & ~(1 << 2), u.y().driven(), "selezione 2: Y2 bassa");
    }
}
