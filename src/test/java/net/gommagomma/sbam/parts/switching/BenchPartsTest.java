package net.gommagomma.sbam.parts.switching;

import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.parts.display.LedBar;
import net.gommagomma.sbam.parts.passive.BussedNetwork;
import net.gommagomma.sbam.parts.passive.IsolatedNetwork;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Componenti da banco: dip switch e pulsante azionati da stimoli, reti di resistenze, barra di LED. */
class BenchPartsTest
{
    private Engine engine;
    private Wire rail, ground;

    private void bench()
    {
        engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.001));
        rail = new Wire("+5V", 100e-12);
        ground = new Wire("GND", 100e-12);
        vcc.out().connect(rail);
        gnd.out().connect(ground);
    }

    @Test
    void dipSwitchWithPullUpsGivesTheComplementOfTheLevers()
    {
        bench();
        DipSwitch sw = engine.add(new DipSwitch("SW", 4,
                new Stimulus(0, 0b0011), new Stimulus(2_000_000, 0b1100)));
        BussedNetwork rn = engine.add(new BussedNetwork("RN", 4, 10_000));
        Bus in = Bus.of("IN", 4, 10e-12);
        sw.a().connect(in);
        rn.r().connect(in);
        rn.common().connect(rail);
        sw.b().connect(ground);

        engine.runUntil(1_500_000);                // pull-up: 10 kohm su ~12 pF, tau ~120 ns
        assertEquals(0b0011, sw.position(), "posizione all'accensione");
        assertTrue(in.get(0).volts() < 0.01 && in.get(1).volts() < 0.01, "levette ON: a massa");
        assertTrue(in.get(2).volts() > 4.99 && in.get(3).volts() > 4.99, "levette OFF: tirate su");
        engine.runUntil(3_500_000);
        assertEquals(0b1100, sw.position());
        assertTrue(in.get(0).volts() > 4.99 && in.get(3).volts() < 0.01, "spostate a 2 us");
        assertEquals(0, engine.unsettledTicks(), "la rete si assesta a ogni tick");
    }

    @Test
    void stimuliMustBeInTimeOrder()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new DipSwitch("SW", 2, new Stimulus(100, 1), new Stimulus(100, 2)));
        assertThrows(IllegalArgumentException.class, () -> new Stimulus(-1, 0));
    }

    @Test
    void aPushButtonClosesOnlyWhilePressed()
    {
        bench();
        PushButton b = engine.add(new PushButton("S1", new Stimulus(2_000_000, 1), new Stimulus(3_000_000, 0)));
        BussedNetwork rn = engine.add(new BussedNetwork("RN", 1, 10_000));
        Wire line = new Wire("LINE", 10e-12);
        b.a().connect(line);
        b.b().connect(ground);
        rn.r().connect(Bus.of("X", 1, 1e-12));
        rn.r().get(0).connect(line);
        rn.common().connect(rail);

        engine.runUntil(1_900_000);
        assertTrue(line.volts() > 4.9, "rilasciato: alto");
        engine.runUntil(2_500_000);
        assertTrue(line.volts() < 0.01, "premuto: a massa");
        engine.runUntil(5_000_000);
        assertTrue(line.volts() > 4.9, "rilasciato di nuovo: risale con il pull-up");
    }

    @Test
    void ledsLightWithEnoughCurrentThroughTheirResistors()
    {
        bench();
        IsolatedNetwork rn = engine.add(new IsolatedNetwork("RN", 2, 470));
        LedBar bar = engine.add(new LedBar("D", 2));
        Wire half = new Wire("HALF", 10e-12);
        Supply low = engine.add(new Supply("V2", 2.0, 0.001));       // sotto la soglia del LED
        low.out().connect(half);
        rn.a().get(0).connect(rail);
        rn.a().get(1).connect(half);
        rn.b().connect(Bus.of("K", 2, 5e-12));
        bar.a().connect(Bus.of("AN", 2, 5e-12));
        rn.b().get(0).connect(bar.a().get(0).wires().get(0));
        rn.b().get(1).connect(bar.a().get(1).wires().get(0));
        bar.k().connect(ground);

        engine.runUntil(100_000);
        double expected = (5.0 - 2.0) / (470 + 15);
        assertEquals(expected, bar.current(0), expected * 0.01, "(5 V - 2 V) / (470 + 15) ohm");
        assertTrue(bar.lit(0));
        assertFalse(bar.lit(1), "2 V non bastano");
        assertEquals(0b01, bar.litMask());
    }
}
