package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.instrument.digital.ContentionSentinel;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.instrument.digital.PortReadSignal;
import net.gommagomma.sbam.instrument.digital.RatingSentinel;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.hardware.display.LedBar;
import net.gommagomma.sbam.hardware.logic.Transceiver;
import net.gommagomma.sbam.hardware.memory.Memory;
import net.gommagomma.sbam.hardware.passive.BussedNetwork;
import net.gommagomma.sbam.hardware.passive.IsolatedNetwork;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.hardware.switching.DipSwitch;
import net.gommagomma.sbam.hardware.switching.PushButton;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Wire;

import java.util.ArrayList;
import java.util.List;

/**
 * Una RAM 62256 provata al banco, tutta a mano: nessuna CPU, solo levette, pulsanti e LED.
 *
 *     SW1 (15 vie) + pull-up --------------------------------- RAM.A0..A14
 *     SW2 (8 vie)  + pull-up -- U1.A  U1.B ------------------- RAM.D0..D7 -- U2.A  U2.B -- 470 ohm -- LED
 *     SW3.0 + pull-up -- U1.G#   (ON: U1 scrive il dato delle levette sul bus della RAM)
 *     SW3.1 + pull-up -- RAM.OE#  (ON: la RAM legge sul bus)
 *     S1    + pull-up -- RAM.WE#  (premuto: impulso di scrittura)
 *     RAM.CE# a massa; U1 e U2 sempre da A verso B; U2 sempre abilitato
 *
 * Una levetta ON porta la linea a massa: per scrivere un valore si mettono le levette al suo complemento,
 * come al banco. Chi opera (lo stimolo, un array per ogni organo) scrive quattro celle e le rilegge;
 * la barra di LED mostra il bus dati della RAM.
 */
public final class RamBench
{
    private static final long STEP = 1_000_000;   // l'operatore fa una mossa ogni microsecondo

    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final int[] cells;
        final int[] values;
        final long[] readAt;
        final Memory ram;
        final Transceiver u1;
        final LedBar d1;

        Circuit()
        {
            cells = new int[] { 0x0000, 0x0001, 0x1234, 0x7FFF };
            values = new int[] { 0x55, 0xAA, 0x0F, 0xF0 };

            // ------------------------------------------------ ciò che fa l'operatore, mossa per mossa
            List<Stimulus> address = new ArrayList<>(), data = new ArrayList<>(), control = new ArrayList<>(), we = new ArrayList<>();
            long t = 0;
            control.add(new Stimulus(0, 0));             // tutto spento
            we.add(new Stimulus(0, 0));
            for (int i = 0; i < cells.length; i++) {      // scritture
                address.add(new Stimulus(t, ~cells[i] & 0x7FFF));
                data.add(new Stimulus(t, ~values[i] & 0xFF));
                control.add(new Stimulus(t + STEP, 0b01));            // U1 abilitato: il dato va sul bus
                we.add(new Stimulus(t + 2 * STEP, 1));                // premo WE
                we.add(new Stimulus(t + 3 * STEP, 0));                // rilascio: la RAM memorizza
                control.add(new Stimulus(t + 4 * STEP, 0b00));        // U1 di nuovo spento
                t += 5 * STEP;
            }
            control.add(new Stimulus(t, 0b10));          // OE# basso: da qui la RAM legge sul bus
            readAt = new long[cells.length];
            for (int i = 0; i < cells.length; i++) {      // letture
                address.add(new Stimulus(t + STEP, ~cells[i] & 0x7FFF));
                readAt[i] = t + 2 * STEP - STEP / 10;    // guardo i LED poco prima della mossa successiva
                t += STEP;
            }

            // ------------------------------------------------ il banco
            sim = new Simulation("ram-bench", 1_000);   // 1 ns
            Supply vcc = sim.add(new Supply("VCC", 5.0, 0.001));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 200e-12);
            Wire ground = new Wire("GND", 200e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            DipSwitch sw1 = sim.add(new DipSwitch("SW1", 15, address.toArray(new Stimulus[0])));
            DipSwitch sw2 = sim.add(new DipSwitch("SW2", 8, data.toArray(new Stimulus[0])));
            DipSwitch sw3 = sim.add(new DipSwitch("SW3", 2, control.toArray(new Stimulus[0])));
            PushButton s1 = sim.add(new PushButton("S1", we.toArray(new Stimulus[0])));
            BussedNetwork rn1 = sim.add(new BussedNetwork("RN1", 15, 10_000));
            BussedNetwork rn2 = sim.add(new BussedNetwork("RN2", 8, 10_000));
            BussedNetwork rn3 = sim.add(new BussedNetwork("RN3", 3, 10_000));
            u1 = sim.add(new Transceiver("U1", Families.HC, 8, 9_000, 15_000, 12_000));   // 74HC245
            Transceiver u2 = sim.add(new Transceiver("U2", Families.HC, 8, 9_000, 15_000, 12_000));   // 74HC245
            ram = sim.add(new Memory("U3", Families.HCT, 15, 8, 70_000, 35_000, 25_000));   // 62256-70
            IsolatedNetwork rn4 = sim.add(new IsolatedNetwork("RN4", 8, 470));
            d1 = sim.add(new LedBar("D1", 8));

            Bus a = Bus.of("ADDR", 15, 10e-12);
            Bus in = Bus.of("DIN", 8, 10e-12);
            Bus d = Bus.of("DATA", 8, 10e-12);
            Bus out = Bus.of("DOUT", 8, 10e-12);
            Bus leds = Bus.of("LED", 8, 5e-12);
            Bus ctl = Bus.of("CTL", 3, 10e-12);        // G#, OE#, WE#

            sw1.a().connect(a);  rn1.r().connect(a);  ram.address().connect(a);
            sw2.a().connect(in); rn2.r().connect(in); u1.a().connect(in);
            u1.b().connect(d);   ram.data().connect(d); u2.a().connect(d);
            u2.b().connect(out); rn4.a().connect(out); rn4.b().connect(leds); d1.a().connect(leds);
            rn3.r().connect(ctl);
            sw3.a().get(0).connect(ctl.get(0));  u1.oe().connect(ctl.get(0));
            sw3.a().get(1).connect(ctl.get(1));  ram.oe().connect(ctl.get(1));
            s1.a().connect(ctl.get(2));          ram.we().connect(ctl.get(2));

            for (BussedNetwork rn : new BussedNetwork[] { rn1, rn2, rn3 }) rn.common().connect(rail);
            sw1.b().connect(ground);
            sw2.b().connect(ground);
            sw3.b().connect(ground);
            s1.b().connect(ground);
            d1.k().connect(ground);
            for (Transceiver u : new Transceiver[] { u1, u2 }) {
                u.vdd().connect(rail);
                u.gnd().connect(ground);
                u.dir().connect(rail);           // da A verso B
            }
            u2.oe().connect(ground);             // U2 sempre abilitato: i LED guardano il bus
            ram.vdd().connect(rail);
            ram.gnd().connect(ground);
            ram.ce().connect(ground);

            sim.log().echoTo(System.out);
            sim.add(new ContentionSentinel(sim.log()));
            sim.add(new RatingSentinel(sim.log(), 20_000));
            sim.vcd("bench")
                    .add(new PortReadSignal(ram.address())).add(new PortReadSignal(ram.data()))
                    .add(new PortDriveSignal(u1.b())).add(new PortDriveSignal(ram.data()))
                    .add(new LevelSignal(ram.we())).add(new LevelSignal(ram.oe())).add(new LevelSignal(u1.oe()));
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
                    .probe(new PortReadSignal(c.ram.address()), "indirizzo (interruttori SW1)")
                    .probe(new PortDriveSignal(c.u1.b()), "dato scritto (dal 245 U1)")
                    .probe(new PortDriveSignal(c.ram.data()), "dato letto (dalla RAM)")
                    .probe(new PortReadSignal(c.ram.data()), "bus dati")
                    .probe(new LevelSignal(c.ram.we()), "WE#  scrittura (pulsante)")
                    .probe(new LevelSignal(c.ram.oe()), "OE#  lettura")
                    .probe(new LevelSignal(c.u1.oe()), "G# del 245 U1");
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
        int[] cells = circuit.cells, values = circuit.values;
        long[] readAt = circuit.readAt;
        LedBar d1 = circuit.d1;

        // ------------------------------------------------ la prova
        int errors = 0;
        for (int i = 0; i < cells.length; i++) {
            sim.runUntil(readAt[i]);
            long seen = d1.litMask();
            boolean ok = seen == values[i];
            if (!ok) errors++;
            System.out.printf("  cella %04X: scritto %02X, LED %02X %s%n", cells[i], values[i], seen, ok ? "ok" : "ERRATO");
        }
        System.out.println("  letture errate: " + errors + ", scontri: " + sim.log().count(Severity.KABOOM));
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }

}
