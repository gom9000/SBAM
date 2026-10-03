package net.gommagomma.sbam.physics;

import net.gommagomma.sbam.fixtures.Fixtures.Load;
import net.gommagomma.sbam.fixtures.Fixtures.Toggler;
import net.gommagomma.sbam.hardware.power.Supply;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una rete delle dimensioni di "Il Ferro": 100 linee, ciascuna con un'uscita che commuta
 * e tre ingressi, più l'alimentazione. Stampa i tick al secondo.
 * La soglia è volutamente larga: serve a scoprire un crollo, non a misurare la macchina.
 */
@Tag("performance")
class PerformanceTest
{
    @Test
    void ferroSizedNetworkRunsFastEnough()
    {
        int lines = 100;
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.05));
        Wire rail = new Wire("+5V", 100e-9);
        vcc.out().connect(rail);
        for (int i = 0; i < lines; i++) {
            Toggler t = engine.add(new Toggler("T" + i, 50_000 + 1_000L * i));
            t.vdd.connect(rail);
            Wire w = new Wire("N" + i, 15e-12);
            t.out.connect(w);
            Load l = engine.add(new Load("L" + i, 3));
            for (Pin p : l.in) p.connect(w);
        }
        engine.compile();
        int pins = 0;
        for (Node n : engine.nodes()) pins += n.pins().size();

        engine.runUntil(20_000_000);                 // riscaldamento del JIT
        int ticks = 100_000;
        long iterations = 0;
        long t0 = System.nanoTime();
        for (int i = 0; i < ticks; i++) {
            engine.step();
            iterations += engine.lastIterations();
        }
        double seconds = (System.nanoTime() - t0) / 1e9;
        double rate = ticks / seconds;

        System.out.printf("PerformanceTest: %d nodi, %d pin, %d device: %.0f tick/s, %.2f tentativi medi%n",
                engine.nodes().size(), pins, engine.devices().size(), rate, (double) iterations / ticks);

        assertEquals(101, engine.nodes().size());
        assertEquals(0, engine.unsettledTicks());
        assertTrue(rate > 5_000, "almeno 5 000 tick al secondo");
    }
}
