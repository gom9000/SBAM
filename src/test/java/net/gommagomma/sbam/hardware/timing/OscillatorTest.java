package net.gommagomma.sbam.hardware.timing;

import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.fixtures.Recorders.RisingDrive;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OscillatorTest
{
    @Test
    void oscillatesAtItsFrequencyAfterTheStartTime()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Oscillator x1 = engine.add(new Oscillator("X1", Families.HC_GATE, 10e6, 0.5, 1_000_000));   // 10 MHz, avvio 1 us
        x1.vdd().connect(rail);
        x1.gnd().connect(Ground.of(engine));
        x1.out().connect(new Wire("CLK", 10e-12));
        RisingDrive rises = engine.add(new RisingDrive(x1.out()));
        engine.runUntil(3_000_000);

        List<Long> t = rises.times();
        assertEquals(1_000_000L, (long) t.get(0), "primo fronte dopo l'avvio");
        assertEquals(21, t.size(), "da 1 a 3 us compresi, a 10 MHz");
        assertEquals(100_000L, t.get(1) - t.get(0), "periodo 100 ns");
    }
}
