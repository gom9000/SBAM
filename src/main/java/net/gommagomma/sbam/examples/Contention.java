package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.logic.LogicFunction;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Event;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.digital.ContentionSentinel;
import net.gommagomma.sbam.instrument.digital.DriveSignal;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.instrument.digital.RatingSentinel;
import net.gommagomma.sbam.instrument.physics.CurrentSentinel;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.parts.logic.Gate;
import net.gommagomma.sbam.parts.passive.Resistor;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Wire;

/**
 * Due uscite 74HC14 sulla stessa linea, l'errore classico di un bus senza arbitro.
 * U1 ha l'ingresso a +5 V: vuole L. U2 ha l'ingresso su un RC che si carica: all'inizio vuole H,
 * dopo circa 8 us (quando il condensatore supera la soglia alta) vuole L anche lui.
 *
 * La sentinella di scontro vede la causa (le intenzioni H e L sullo stesso nodo),
 * quella di specifica il sintomo sulle uscite (ben oltre i 6 mA garantiti dall'HC),
 * quella di corrente il sintomo fisico, anche sull'alimentazione.
 * In simulations/contention/: lines.vcd (da aprire con GTKWave), events.txt e run.txt.
 */
public final class Contention
{
    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Gate u1;
        final Gate u2;
        final Wire slow;
        final Wire line;

        Circuit()
        {
            sim = new Simulation("contention", 10_000);   // 10 ns

            Supply vcc = sim.add(new Supply("VCC", 5.0, 0.1));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 100e-12);
            Wire ground = new Wire("GND", 100e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            u1 = sim.add(new Gate("U1", Families.HC_SCHMITT_GATE, LogicFunction.NOT, 1, 13_000));   // 74HC14
            u2 = sim.add(new Gate("U2", Families.HC_SCHMITT_GATE, LogicFunction.NOT, 1, 13_000));   // 74HC14
            Resistor r = sim.add(new Resistor("R", 10_000));
            slow = new Wire("RC", 1e-9);
            line = new Wire("LINE", 10e-12);

            u1.vdd().connect(rail);
            u1.gnd().connect(ground);
            u2.vdd().connect(rail);
            u2.gnd().connect(ground);
            u1.in(0).connect(rail);
            r.a().connect(rail);
            r.b().connect(slow);
            u2.in(0).connect(slow);
            u1.y().connect(line);
            u2.y().connect(line);

            sim.log().echoTo(System.out);
            sim.add(new ContentionSentinel(sim.log()));
            sim.add(new RatingSentinel(sim.log(), 20_000));
            sim.add(new CurrentSentinel(sim.log(), 20e-3));
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
                    .probe(new LevelSignal(c.u2.in(0)), "ingresso di U2 (sale lento)")
                    .probe(new DriveSignal(c.u1.y()), "uscita di U1")
                    .probe(new DriveSignal(c.u2.y()), "uscita di U2")
                    .probe(new VoltageSignal(c.slow), "tensione RC (ingresso di U2)")
                    .probe(new VoltageSignal(c.line), "tensione della linea comune")
                    .probe(new CurrentSignal(c.u1.y()), "corrente nell'uscita di U1");
            return c.sim;
        }
    }

    public static void main(String[] args)
    {
        if (args.length > 0 && args[0].equals("--live")) {              // a banco: la finestra della simulazione
            new SimulationWindow(new Live()).show();
            return;
        }
        Circuit circuit = new Circuit();
        Simulation sim = circuit.sim;
        Gate u1 = circuit.u1, u2 = circuit.u2;
        Wire slow = circuit.slow, line = circuit.line;

        ChangeTrace changes = new ChangeTrace();
        sim.add(new Recorder(changes)
                .add(new LevelSignal(u2.in(0))).add(new DriveSignal(u1.y())).add(new DriveSignal(u2.y())));
        sim.vcd("lines")
                .add(new DriveSignal(u1.y())).add(new DriveSignal(u2.y())).add(new LevelSignal(u2.in(0)))
                .add(new VoltageSignal(slow)).add(new VoltageSignal(line));

        sim.runUntil(20_000_000);

        System.out.println();
        System.out.println("sonda logica (solo i cambiamenti):");
        for (int i = 0; i < changes.size(); i++) {
            System.out.printf("  [%12s] %-12s %c%n", Event.formatTime(changes.time(i)),
                    changes.columns().get(changes.column(i)), changes.logic(i));
        }
        System.out.println();
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }
}
