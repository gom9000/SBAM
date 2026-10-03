package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.instrument.digital.PortReadSignal;
import net.gommagomma.sbam.instrument.logic.TimingSentinel;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.hardware.display.LedBar;
import net.gommagomma.sbam.hardware.logic.Register;
import net.gommagomma.sbam.hardware.passive.BussedNetwork;
import net.gommagomma.sbam.hardware.passive.IsolatedNetwork;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.hardware.switching.DipSwitch;
import net.gommagomma.sbam.hardware.switching.PushButton;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Wire;

/**
 * Al banco: un registro 74HC574 caricato a mano.
 *
 *     SW1 (8 vie) + pull-up -- U1.D     U1.Q -- 470 ohm -- LED
 *     S1 + pull-up ----------- U1.CP    (rilasciando il pulsante, CP sale: il registro cattura D)
 *     U1.OE# a massa
 *
 * Le levette cambiano D quando si vuole, ma i LED cambiano solo quando si rilascia il pulsante.
 * L'ultima volta l'operatore sposta le levette 10 ns prima di rilasciare il pulsante. CP sale con il suo
 * pull-up, ~190 ns dopo il rilascio; le linee di D che passano da 0 a 1 salgono con lo stesso tipo di
 * pull-up, partite 10 ns prima: al fronte sono stabili da appena 10 ns (le linee che scendono, chiuse a
 * massa dal contatto, sono stabili subito). La sentinella di setup e hold lo segnala (BOING): il valore
 * catturato non è garantito.
 */
public final class RegisterBench
{
    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Register u1;
        final LedBar d1;
        final Bus d;
        final Wire cp;

        Circuit()
        {
            // levette ON = 0 sul D; il pulsante premuto = CP basso
            Stimulus[] levers = {
                    new Stimulus(0, 0xFF),             // D = 00
                    new Stimulus(2_000_000, 0x0F),     // D = F0
                    new Stimulus(5_000_000, 0x3C),     // D = C3
                    new Stimulus(7_990_000, 0xAA),     // D = 55, 10 ns prima del rilascio
            };
            Stimulus[] button = {
                    new Stimulus(1_000_000, 1), new Stimulus(1_500_000, 0),
                    new Stimulus(3_000_000, 1), new Stimulus(3_500_000, 0),
                    new Stimulus(6_000_000, 1), new Stimulus(6_500_000, 0),
                    new Stimulus(7_500_000, 1), new Stimulus(8_000_000, 0),
            };

            sim = new Simulation("register-bench", 1_000);   // 1 ns
            Supply vcc = sim.add(new Supply("VCC", 5.0, 0.001));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 200e-12);
            Wire ground = new Wire("GND", 200e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            DipSwitch sw1 = sim.add(new DipSwitch("SW1", 8, levers));
            PushButton s1 = sim.add(new PushButton("S1", button));
            BussedNetwork rn1 = sim.add(new BussedNetwork("RN1", 9, 10_000));     // 8 per D, 1 per CP
            u1 = sim.add(new Register("U1", Families.HC, 8, 14_000, 14_000, 12_000, 12_000, 3_000));   // 74HC574
            IsolatedNetwork rn2 = sim.add(new IsolatedNetwork("RN2", 8, 470));
            d1 = sim.add(new LedBar("D1", 8));

            d = Bus.of("D", 8, 10e-12);
            Bus q = Bus.of("Q", 8, 10e-12);
            Bus leds = Bus.of("LED", 8, 5e-12);
            cp = new Wire("CP", 10e-12);
            sw1.a().connect(d);
            rn1.r().slice(0, 8).connect(d);
            rn1.r().get(8).connect(cp);
            rn1.common().connect(rail);
            s1.a().connect(cp);
            s1.b().connect(ground);
            sw1.b().connect(ground);
            u1.vdd().connect(rail);
            u1.gnd().connect(ground);
            u1.d().connect(d);
            u1.cp().connect(cp);
            u1.oe().connect(ground);
            u1.q().connect(q);
            rn2.a().connect(q);
            rn2.b().connect(leds);
            d1.a().connect(leds);
            d1.k().connect(ground);

            sim.log().echoTo(System.out);
            sim.add(new TimingSentinel(sim.log()));
            sim.vcd("register")
                    .add(new PortReadSignal(u1.d())).add(new LevelSignal(u1.cp())).add(new PortDriveSignal(u1.q()))
                    .add(new VoltageSignal(d.get(0))).add(new VoltageSignal(cp));
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
                    .probe(new PortReadSignal(c.u1.d()), "D (dagli interruttori)")
                    .probe(new LevelSignal(c.u1.cp()), "CP (pulsante)")
                    .probe(new PortDriveSignal(c.u1.q()), "Q (verso i LED)")
                    .probe(new VoltageSignal(c.d.get(0)), "tensione su D0")
                    .probe(new VoltageSignal(c.cp), "tensione su CP");
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
        Register u1 = circuit.u1;
        LedBar d1 = circuit.d1;

        long[] look = { 900_000, 2_500_000, 4_000_000, 7_000_000, 9_000_000 };
        System.out.println("  istante     D letto   LED");
        for (long t : look) {
            sim.runUntil(t);
            System.out.printf("  %5.1f us    %02X        %02X%n", t / 1e6, u1.d().read(), d1.litMask());
        }
        System.out.println("  BOING: " + sim.log().count(Severity.BOING));
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }

}
