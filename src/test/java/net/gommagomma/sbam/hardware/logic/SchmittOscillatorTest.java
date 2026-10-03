package net.gommagomma.sbam.hardware.logic;

import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.fixtures.Recorders.RisingDrive;
import net.gommagomma.sbam.logic.LogicFunction;
import net.gommagomma.sbam.hardware.passive.Resistor;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Oscillatore RC con una porta 74HC14 (scritta sullo strato digitale): R dall'uscita all'ingresso,
 * C dall'ingresso a massa. Il periodo si confronta con la teoria e con la versione precedente,
 * scritta direttamente sullo strato fisico (9,220 us).
 */
class SchmittOscillatorTest
{
    private static final double VDD = 5.0, VTP = 0.55 * VDD, VTM = 0.33 * VDD;
    private static final double RH = (4.5 - 3.84) / 6e-3, RL = 0.33 / 6e-3;   // dagli stadi 74HC
    private static final double R = 10_000, C = 1e-9;
    private static final double C_IN = C + 3.5e-12;
    private static final double PREVIOUS_PERIOD = 9.220e-6;

    private static List<Long> risingEdges(long tickPs, long untilPs, long delayPs)
    {
        Engine engine = new Engine(tickPs);
        Supply vcc = engine.add(new Supply("VCC", VDD, 0.1));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);

        Gate u1 = engine.add(new Gate("U1", Families.HC_SCHMITT_GATE, LogicFunction.NOT, 1, delayPs));
        Resistor rf = engine.add(new Resistor("R", R));
        Wire in = new Wire("IN", C);
        Wire out = new Wire("OUT", 10e-12);
        u1.vdd().connect(rail);
        u1.gnd().connect(Ground.of(engine));
        u1.in(0).connect(in);
        rf.b().connect(in);
        u1.y().connect(out);
        rf.a().connect(out);

        RisingDrive rises = engine.add(new RisingDrive(u1.y()));
        engine.runUntil(untilPs);
        return rises.times();
    }

    private static double period(List<Long> rises)
    {
        int n = rises.size();
        return (rises.get(n - 1) - rises.get(2)) * 1e-12 / (n - 3);
    }

    /** Periodo teorico a regime con un ritardo d tra superamento della soglia e commutazione. */
    private static double theory(double d)
    {
        double tauH = (R + RH) * C_IN, tauL = (R + RL) * C_IN;
        double v1 = VDD - (VDD - VTP) * Math.exp(-d / tauH);
        double v2 = VTM * Math.exp(-d / tauL);
        return d + tauL * Math.log(v1 / VTM) + d + tauH * Math.log((VDD - v2) / (VDD - VTP));
    }

    @Test
    void oscillatesWithThePeriodOfTheFormula()
    {
        long tick = 10_000;
        List<Long> rises = risingEdges(tick, 200_000_000, 0);
        assertTrue(rises.size() > 15, "oscilla: " + rises.size() + " fronti");
        double expected = theory(0.5 * tick * 1e-12);
        assertEquals(expected, period(rises), expected * 0.005, "entro lo 0,5% della teoria");
    }

    @Test
    void matchesThePreviousPhysicalLayerImplementation()
    {
        double p = period(risingEdges(10_000, 200_000_000, 0));
        assertEquals(PREVIOUS_PERIOD, p, PREVIOUS_PERIOD * 0.003, "entro lo 0,3% della versione precedente");
    }

    @Test
    void propagationDelayLengthensThePeriodAsPredicted()
    {
        long tick = 10_000;
        long delay = 190_000;   // esagerato apposta, multiplo del tick
        // il ritardo, più la mezza latenza di soglia (il superamento avviene in media a metà tick)
        double d = (delay + 0.5 * tick) * 1e-12;

        double slow = period(risingEdges(tick, 200_000_000, delay));
        double fast = period(risingEdges(tick, 200_000_000, 0));
        assertEquals(theory(d), slow, theory(d) * 0.005, "entro lo 0,5% della teoria con ritardo");
        assertTrue(slow > fast + 2 * 190e-9, "più di due volte il ritardo: il condensatore oltrepassa la soglia");
    }
}
