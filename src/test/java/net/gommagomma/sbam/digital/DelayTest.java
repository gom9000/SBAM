package net.gommagomma.sbam.digital;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Follower;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.fixtures.Recorders.DriveChanges;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Un ritardo di propagazione come intenzione in attesa sul pin: vince l'ultima decisione. */
class DelayTest
{
    /** Sorgente -> follower con ritardo -> filo; restituisce gli istanti in cui l'uscita cambia intenzione. */
    private static List<Long> follow(long pulseStartPs, long pulseEndPs, long delayPs)
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Source src = engine.add(new Source("SRC", Families.HC_TRISTATE)
                .at(0, Drive.L).at(pulseStartPs, Drive.H).at(pulseEndPs, Drive.L));
        Follower f = engine.add(new Follower("F", Families.HC_IN, Families.HC_TRISTATE, delayPs));
        Wire a = new Wire("A", 10e-12);
        Wire y = new Wire("Y", 10e-12);
        src.vdd.connect(rail);
        src.gnd.connect(Ground.of(engine));
        f.vdd.connect(rail);
        f.gnd.connect(Ground.of(engine));
        src.out.connect(a);
        f.in.connect(a);
        f.out.connect(y);

        DriveChanges changes = engine.add(new DriveChanges(f.out));
        engine.runUntil(2_000_000);
        return changes.times();
    }

    @Test
    void outputFollowsAfterTheDelay()
    {
        List<Long> changes = follow(200_000, 700_000, 100_000);   // impulso di 500 ns, ritardo 100 ns
        // la prima commutazione è l'avvio (da Z a L); poi su e giù 100 ns dopo i fronti dell'ingresso
        assertEquals(3, changes.size(), "avvio, salita, discesa: " + changes);
        long rise = changes.get(1), fall = changes.get(2);
        assertTrue(rise >= 300_000 && rise <= 305_000, "salita ~100 ns dopo 200 ns: " + rise);
        assertTrue(fall >= 800_000 && fall <= 805_000, "discesa ~100 ns dopo 700 ns: " + fall);
    }

    @Test
    void pulseShorterThanTheDelayDoesNotPassThrough()
    {
        List<Long> changes = follow(200_000, 230_000, 100_000);   // impulso di 30 ns, ritardo 100 ns
        assertEquals(1, changes.size(), "solo l'avvio: la decisione è stata sostituita: " + changes);
    }
}
