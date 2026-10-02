package net.gommagomma.sbam.parts.logic;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.logic.LogicFunction;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Una porta NAND a due ingressi: tabella di verità e ritardo. */
class GateTest
{
    @Test
    void nandTruthTableWithItsDelay()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Wire ground = Ground.of(engine);
        Gate g = engine.add(new Gate("U1A", Families.HC_GATE, LogicFunction.NAND, 2, 50_000));
        g.vdd().connect(rail);
        g.gnd().connect(ground);
        // A: L L H H, B: L H L H, ogni 200 ns
        Source a = engine.add(new Source("SA", Families.HC_TRISTATE).at(0, Drive.L).at(400_000, Drive.H));
        Source b = engine.add(new Source("SB", Families.HC_TRISTATE).at(0, Drive.L).at(200_000, Drive.H)
                .at(400_000, Drive.L).at(600_000, Drive.H));
        for (Source s : new Source[] { a, b }) {
            s.vdd.connect(rail);
            s.gnd.connect(ground);
        }
        Wire wa = new Wire("A", 5e-12), wb = new Wire("B", 5e-12), wy = new Wire("Y", 5e-12);
        a.out.connect(wa);
        g.in(0).connect(wa);
        b.out.connect(wb);
        g.in(1).connect(wb);
        g.y().connect(wy);

        for (int i = 0; i < 3; i++) {
            engine.runUntil(i * 200_000 + 150_000);
            assertEquals(Drive.H, g.y().driven(), "riga " + i + ": almeno un ingresso basso");
        }
        engine.runUntil(640_000);
        assertEquals(Drive.H, g.y().driven(), "entrambi alti da 40 ns: il ritardo è di 50");
        engine.runUntil(700_000);
        assertEquals(Drive.L, g.y().driven(), "riga 3: entrambi alti");
    }
}
