package net.gommagomma.sbam.physics;

import net.gommagomma.sbam.fixtures.Fixtures.Impure;
import net.gommagomma.sbam.fixtures.Fixtures.Switch;
import net.gommagomma.sbam.fixtures.Fixtures.Thief;
import net.gommagomma.sbam.fixtures.Fixtures.Twin;
import net.gommagomma.sbam.hardware.passive.Resistor;
import net.gommagomma.sbam.hardware.power.Supply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineTest
{
    private static final double TAU = 1_000 * 1e-9;   // 1 kohm x 1 nF

    /** 5 V, 1 kohm, 1 nF: restituisce il filo del condensatore dopo la compilazione. */
    private static Wire rc(Engine engine)
    {
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Resistor r = engine.add(new Resistor("R1", 1_000));
        Wire rail = new Wire("+5V", 10e-12);
        Wire vc = new Wire("VC", 1e-9);
        vcc.out().connect(rail);
        r.a().connect(rail);
        r.b().connect(vc);
        return vc;
    }

    private static double exact(long ps) { return 5.0 * (1.0 - Math.exp(-(ps * 1e-12) / TAU)); }

    @Test
    void rcChargeFollowsTheExponentialWithTickTauOver100()
    {
        Engine engine = new Engine(10_000);
        Wire vc = rc(engine);
        engine.runUntil(1_000_000);   // t = tau
        assertEquals(exact(1_000_000), vc.volts(), 5.0 * 0.005, "entro lo 0,5% del valore finale");
    }

    @Test
    void rcErrorShrinksWithTheTick()
    {
        Engine coarse = new Engine(10_000);
        Engine fine = new Engine(1_000);
        Wire a = rc(coarse);
        Wire b = rc(fine);
        coarse.runUntil(1_000_000);
        fine.runUntil(1_000_000);
        double errCoarse = Math.abs(a.volts() - exact(1_000_000));
        double errFine = Math.abs(b.volts() - exact(1_000_000));
        assertTrue(errFine < errCoarse / 5, "tick 10 volte più fine, errore molto più piccolo");
        assertEquals(exact(1_000_000), b.volts(), 5.0 * 0.0005, "tick tau/1000: entro lo 0,05%");
    }

    @Test
    void nodesStartAtZeroVolts()
    {
        Engine engine = new Engine(10_000);
        Wire vc = rc(engine);
        engine.compile();
        assertEquals(0.0, vc.volts(), 0.0);
        engine.step();
        assertTrue(vc.volts() > 0.0 && vc.volts() < 0.1, "dopo un tick il condensatore ha appena iniziato a caricarsi");
    }

    @Test
    void floatingNodeKeepsItsCharge()
    {
        Engine engine = new Engine(1_000);
        Switch sw = engine.add(new Switch("SW", 5.0, 100.0, 200_000));
        Wire line = new Wire("LINE", 15e-12);
        sw.out.connect(line);

        engine.runUntil(100_000);
        assertEquals(5.0, line.volts(), 1e-3);
        assertFalse(line.node().isFloating());

        engine.runUntil(1_000_000);
        assertEquals(5.0, line.volts(), 1e-3, "rilasciato: la carica resta sulla capacità");
        assertTrue(line.node().isFloating());
    }

    @Test
    void pinCurrentIsMeasuredAfterSettling()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.1));
        Resistor r = engine.add(new Resistor("R", 1_000));
        Wire a = new Wire("A", 10e-12);
        Wire b = new Wire("B", 10e-12);
        vcc.out().connect(a);
        r.a().connect(a);
        r.b().connect(b);
        gnd.out().connect(b);
        engine.runUntil(100_000);

        double expected = 5.0 / 1_000.2;
        assertEquals(expected, vcc.out().current(), 1e-7, "l'alimentatore eroga");
        assertEquals(-expected, gnd.out().current(), 1e-7, "la massa assorbe");
        assertEquals(-expected, r.a().current(), 1e-7, "dal lato alto la resistenza assorbe");
        assertEquals(expected, r.b().current(), 1e-7, "dal lato basso la resistenza eroga");
    }

    @Test
    void samePinDeclaredTwiceAddsInParallel()
    {
        Engine engine = new Engine(1_000);
        Twin t = engine.add(new Twin());
        Wire w = new Wire("W", 10e-12);
        t.out.connect(w);
        engine.runUntil(100_000);
        assertEquals(2.5, w.volts(), 1e-6, "5 V e 0 V con la stessa resistenza: a metà");
    }

    @Test
    void wiresAndPinsTouchingFormOneNode()
    {
        Engine engine = new Engine(1_000);
        Supply s = engine.add(new Supply("S", 5.0, 1.0));
        Resistor r = engine.add(new Resistor("R", 1_000));
        Wire w1 = new Wire("W1", 5e-12);
        Wire w2 = new Wire("W2", 7e-12);
        s.out().connect(w1);
        r.a().connect(w1);
        r.a().connect(w2);      // due fili sullo stesso pin
        r.b().connect(w2);      // la resistenza ha i due capi sullo stesso nodo
        engine.compile();
        assertTrue(w1.node() == w2.node());
        assertEquals(12e-12, w1.node().capacitance(), 1e-18);
        assertEquals(1, engine.nodes().size());
    }

    @Test
    void topologyIsFrozenAfterCompile()
    {
        Engine engine = new Engine(1_000);
        Supply s = engine.add(new Supply("S", 5.0, 1.0));
        s.out().connect(new Wire("W", 1e-12));
        engine.compile();
        assertThrows(IllegalStateException.class, () -> s.out().connect(new Wire("X", 1e-12)));
        assertThrows(IllegalStateException.class, () -> engine.add(new Supply("T", 5.0, 1.0)));
    }

    @Test
    void nodeWithoutCapacitanceIsRejected()
    {
        Engine engine = new Engine(1_000);
        engine.add(new Supply("S", 5.0, 1.0));   // pin senza capacità e senza fili
        IllegalStateException e = assertThrows(IllegalStateException.class, engine::compile);
        assertTrue(e.getMessage().contains("senza capacità"), e.getMessage());
    }

    @Test
    void negativeCapacitanceIsRejected()
    {
        Engine engine = new Engine(1_000);
        Supply s = engine.add(new Supply("S", 5.0, 1.0));
        s.out().connect(new Wire("W", -1e-12));
        assertThrows(IllegalStateException.class, engine::compile);
    }

    @Test
    void deviceCannotDeclareForeignPins()
    {
        Engine engine = new Engine(1_000);
        Thief t = engine.add(new Thief());
        Supply s = engine.add(new Supply("S", 5.0, 1.0));
        Wire w = new Wire("W", 1e-12);
        t.own.connect(w);
        s.out().connect(w);
        t.stolen = s.out();
        IllegalStateException e = assertThrows(IllegalStateException.class, engine::step);
        assertTrue(e.getMessage().contains("solo i propri pin"), e.getMessage());
    }

    @Test
    void purityCheckFindsStatefulReact()
    {
        Engine engine = new Engine(1_000).checkPurity(true);
        Impure d = engine.add(new Impure());
        d.out.connect(new Wire("X", 1e-12));
        assertThrows(IllegalStateException.class, engine::step);
    }

    @Test
    void invalidResistanceIsRejectedEvenAsNaN()
    {
        Engine engine = new Engine(1_000);
        Device bad = engine.add(new Device("BAD")
        {
            final Pin p = pin("P", 1e-12);

            @Override
            protected void react(Tick tick, Voltages trial, Reaction r) { r.set(p, 5.0, Double.NaN, 0.0); }

            @Override
            protected void update(Tick tick, Voltages settled) { }
        });
        assertTrue(bad.pins().size() == 1);
        assertThrows(IllegalStateException.class, engine::step);
    }

    @Test
    void timeAdvancesByTicks()
    {
        Engine engine = new Engine(2_500);
        rc(engine);
        engine.runUntil(10_000);
        assertEquals(10_000L, engine.nowPs());
        engine.step();
        assertEquals(12_500L, engine.nowPs());
    }
}
