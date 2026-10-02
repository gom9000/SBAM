package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.instrument.digital.DriveSignal;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.instrument.logic.TimingSentinel;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.parts.display.LedBar;
import net.gommagomma.sbam.parts.logic.Counter;
import net.gommagomma.sbam.parts.passive.BussedNetwork;
import net.gommagomma.sbam.parts.passive.IsolatedNetwork;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.parts.switching.PushButton;
import net.gommagomma.sbam.parts.timing.Oscillator;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Wire;

/**
 * Un contatore che gira da solo: un oscillatore da 1 MHz sul clock di un 74HC161, quattro LED sulle
 * uscite e uno sul riporto (TC). A metà prova l'operatore preme il pulsante di azzeramento (MR#):
 * il contatore torna a zero subito, senza aspettare il clock, e riparte quando il pulsante si rilascia.
 *
 *     X1 (1 MHz) -- U1.CP    U1.Q0..Q3 -- 470 ohm -- LED0..3     U1.TC -- 470 ohm -- LED4
 *     S1 + pull-up -- U1.MR#;  PE#, CEP, CET a +5 V;  D a massa
 */
public final class CounterLeds
{
    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Counter u1;
        final LedBar d1;
        final Wire clk;

        Circuit()
        {
            sim = new Simulation("counter-leds", 1_000);   // 1 ns
            Supply vcc = sim.add(new Supply("VCC", 5.0, 0.001));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 200e-12);
            Wire ground = new Wire("GND", 200e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            Oscillator x1 = sim.add(new Oscillator("X1", Families.HC_GATE, 1e6, 0.5, 500_000));
            u1 = sim.add(new Counter("U1", Families.HC_GATE, 4, 16_000, 12_000, 3_000));   // 74HC161
            PushButton s1 = sim.add(new PushButton("S1", new Stimulus(20_300_000, 1), new Stimulus(22_700_000, 0)));
            BussedNetwork rn1 = sim.add(new BussedNetwork("RN1", 1, 10_000));
            IsolatedNetwork rn2 = sim.add(new IsolatedNetwork("RN2", 5, 470));
            d1 = sim.add(new LedBar("D1", 5));

            clk = new Wire("CLK", 10e-12);
            Wire mr = new Wire("MR", 10e-12);
            Bus q = Bus.of("Q", 5, 10e-12);                 // Q0..Q3 e TC
            Bus leds = Bus.of("LED", 5, 5e-12);
            x1.vdd().connect(rail);
            x1.gnd().connect(ground);
            x1.out().connect(clk);
            u1.vdd().connect(rail);
            u1.gnd().connect(ground);
            u1.cp().connect(clk);
            for (Pin p : new Pin[] { u1.pe(), u1.cep(), u1.cet() }) p.connect(rail);
            u1.d().connect(ground);
            rn1.r().get(0).connect(mr);
            rn1.common().connect(rail);
            s1.a().connect(mr);
            s1.b().connect(ground);
            u1.mr().connect(mr);
            u1.q().connect(q.slice(0, 4));
            u1.tc().connect(q.get(4));
            rn2.a().connect(q);
            rn2.b().connect(leds);
            d1.a().connect(leds);
            d1.k().connect(ground);

            sim.log().echoTo(System.out);
            sim.add(new TimingSentinel(sim.log()));
            sim.vcd("counter")
                    .add(new LevelSignal(u1.cp())).add(new LevelSignal(u1.mr()))
                    .add(new PortDriveSignal(u1.q())).add(new DriveSignal(u1.tc()));
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
                    .probe(new LevelSignal(c.u1.cp()), "clock (1 MHz)")
                    .probe(new LevelSignal(c.u1.mr()), "MR#  azzeramento (pulsante)")
                    .probe(new PortDriveSignal(c.u1.q()), "conteggio (Q3..Q0)")
                    .probe(new DriveSignal(c.u1.tc()), "TC  riporto")
                    .probe(new VoltageSignal(c.clk), "tensione del clock");
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
        Counter u1 = circuit.u1;
        LedBar d1 = circuit.d1;

        System.out.println("  istante   conteggio   LED (TC Q3..Q0)");
        for (int us = 1; us <= 30; us += 1) {
            sim.runUntil(us * 1_000_000L + 900_000);      // poco prima del fronte successivo
            long lit = d1.litMask();
            System.out.printf("  %4.1f us   %2d          %s %s%n", us + 0.9, u1.count(),
                    (lit & 0x10) != 0 ? "*" : ".", bits(lit & 0xF));
        }
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }

    private static String bits(long v)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i >= 0; i--) sb.append((v & (1L << i)) != 0 ? '*' : '.');
        return sb.toString();
    }
}
