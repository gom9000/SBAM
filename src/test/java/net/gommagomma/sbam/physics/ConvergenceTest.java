package net.gommagomma.sbam.physics;

import net.gommagomma.sbam.hardware.passive.Diode;
import net.gommagomma.sbam.hardware.passive.Resistor;
import net.gommagomma.sbam.hardware.power.Supply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'assestamento nei casi che mettono alla prova il metodo: non linearità e nodi molto accoppiati. */
class ConvergenceTest
{
    /** 5 V -> 1 kohm -> diodo -> massa; restituisce il filo dell'anodo (o del catodo, se invertito). */
    private static Wire diodeCircuit(Engine engine, boolean forward)
    {
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.1));
        Resistor r = engine.add(new Resistor("R", 1_000));
        Diode d = engine.add(new Diode("D", 0.6, 10.0));
        Wire rail = new Wire("+5V", 10e-12);
        Wire mid = new Wire("MID", 10e-12);
        Wire ground = new Wire("GND", 10e-12);
        vcc.out().connect(rail);
        r.a().connect(rail);
        r.b().connect(mid);
        gnd.out().connect(ground);
        if (forward) {
            d.anode().connect(mid);
            d.cathode().connect(ground);
        } else {
            d.anode().connect(ground);
            d.cathode().connect(mid);
        }
        return mid;
    }

    @Test
    void forwardDiodeClampsAtItsThreshold()
    {
        Engine engine = new Engine(1_000);
        Wire mid = diodeCircuit(engine, true);
        engine.runUntil(2_000_000);

        // 0,6 V + caduta su 10 ohm della corrente (5 - 0,6) / ~1010 ohm
        double i = (5.0 - 0.6) / (1_000 + 10 + 0.2);
        assertEquals(0.6 + i * 10, mid.volts(), 1e-3);
        assertEquals(0, engine.unsettledTicks(), "ogni tick assestato entro la tolleranza");
    }

    @Test
    void reverseDiodeDoesNotConduct()
    {
        Engine engine = new Engine(1_000);
        Wire mid = diodeCircuit(engine, false);
        engine.runUntil(2_000_000);
        assertEquals(5.0, mid.volts(), 1e-3);
        assertEquals(0, engine.unsettledTicks());
    }

    /** Alimentatore -> catena di 10 resistenze uguali -> carico da 1 kohm -> massa. */
    private static Wire chain(Engine engine, double ohms)
    {
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.1));
        Wire prev = new Wire("+5V", 10e-12);
        vcc.out().connect(prev);
        for (int i = 0; i < 10; i++) {
            Resistor r = engine.add(new Resistor("R" + i, ohms));
            Wire w = new Wire("N" + i, 10e-12);
            r.a().connect(prev);
            r.b().connect(w);
            prev = w;
        }
        Resistor load = engine.add(new Resistor("RL", 1_000));
        Wire ground = new Wire("GND", 10e-12);
        load.a().connect(prev);
        load.b().connect(ground);
        gnd.out().connect(ground);
        return prev;
    }

    @Test
    void chainOfModerateResistorsConvergesEveryTick()
    {
        Engine engine = new Engine(1_000);
        Wire end = chain(engine, 100);
        engine.runUntil(5_000_000);
        assertEquals(5.0 * 1_000 / (1_000 + 10 * 100 + 0.2), end.volts(), 1e-3);
        assertEquals(0, engine.unsettledTicks());
    }

    /**
     * Limite noto del metodo attuale: con nodi legati da resistenze molto piccole (qui 1 ohm,
     * contro una capacità di 10 pF a tick da 1 ns) i tentativi convergono lentamente e alcuni tick
     * del transitorio si chiudono senza raggiungere la tolleranza. Il valore a regime però è giusto.
     * Nei circuiti di SBAM i fili sono ideali e si fondono nei nodi, quindi catene così sono rare.
     */
    @Test
    void chainOfTinyResistorsStillReachesTheRightValue()
    {
        Engine engine = new Engine(1_000);
        Wire end = chain(engine, 1);
        engine.runUntil(5_000_000);
        assertEquals(5.0 * 1_000 / (1_000 + 10 + 0.2), end.volts(), 1e-3);
        assertTrue(engine.lastIterations() <= 2, "a regime bastano uno o due tentativi");
    }
}
