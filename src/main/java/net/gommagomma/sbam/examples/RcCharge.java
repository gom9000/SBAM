package net.gommagomma.sbam.examples;

import java.util.Locale;

import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.hardware.passive.Resistor;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.Trace;
import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.physics.Wire;

/**
 * Carica di un condensatore: 5 V, 1 kohm, 1 nF (tau = 1 us), tick da 10 ns.
 * Una sonda registra tensione e corrente in simulations/rc-charge/rc.csv, da aprire
 * in un foglio di calcolo per vedere la curva.
 */
public final class RcCharge
{
    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Resistor r1;
        final Wire vc;

        Circuit()
        {
            sim = new Simulation("rc-charge", 10_000);   // 10 ns

            Supply vcc = sim.add(new Supply("VCC", 5.0, 0.1));
            r1 = sim.add(new Resistor("R1", 1_000));
            Wire rail = new Wire("+5V", 10e-12);
            vc = new Wire("VC", 1e-9);       // il condensatore è la capacità del filo
            vcc.out().connect(rail);
            r1.a().connect(rail);
            r1.b().connect(vc);
        }
    }

    /** Per la finestra: il circuito e le sonde, con i nomi da mostrare. */
    private static final class Live implements Setup
    {
        @Override
        public Simulation build(Probes probes)
        {
            Circuit c = new Circuit();
            probes
                    .probe(new VoltageSignal(c.vc), "tensione sul condensatore")
                    .probe(new CurrentSignal(c.r1.b()), "corrente nella resistenza");
            return c.sim;
        }
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length > 0 && args[0].equals("--live")) {              // a banco: la finestra della simulazione
            new SimulationWindow(new Live()).show();
            return;
        }
        Circuit circuit = new Circuit();
        Simulation sim = circuit.sim;
        Resistor r1 = circuit.r1;
        Wire vc = circuit.vc;

        Trace t = new Trace();
        sim.add(new Recorder(t).add(new VoltageSignal(vc)).add(new CurrentSignal(r1.b())).every(10));
        sim.runUntil(5_000_000);              // 5 tau

        System.out.println("     t        V(VC)     I(R1)     atteso");
        for (int i = 9; i < t.size(); i += 10) {
            double s = t.time(i) * 1e-12;
            double expected = 5.0 * (1 - Math.exp(-s / 1e-6));
            System.out.printf(Locale.ITALIAN, "%10s  %7.4f V  %6.3f mA  %7.4f V%n",
                    Quantities.time(t.time(i)), t.value(0, i), t.value(1, i) * 1000, expected);
        }
        t.writeCsv(sim.file("rc.csv"));
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }
}
