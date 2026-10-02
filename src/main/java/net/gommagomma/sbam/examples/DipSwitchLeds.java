package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.instrument.digital.PortReadSignal;
import net.gommagomma.sbam.instrument.digital.RatingSentinel;
import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.parts.display.LedBar;
import net.gommagomma.sbam.parts.logic.Transceiver;
import net.gommagomma.sbam.parts.passive.BussedNetwork;
import net.gommagomma.sbam.parts.passive.IsolatedNetwork;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.parts.switching.DipSwitch;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Wire;

import java.util.Locale;

/**
 * Al banco: un dip switch a 8 vie con i pull-up, letto da un 74HC245 che accende una barra di LED.
 *
 *     +5V -- RN1 (8 x 10k) --+-- IN0..7 -- U1.A   U1.B -- OUT0..7 -- RN2 (8 x 470) -- LED0..7 -- D1 -- massa
 *                            |
 *                          SW1 (levetta ON = chiusa verso massa)
 *
 * Levetta ON vuol dire ingresso a massa: il LED corrispondente è spento. L'operatore sposta le levette
 * quattro volte (lo stimolo è un array: istante e posizione); la barra mostra il complemento.
 * Aprendo una levetta l'ingresso sale con il pull-up, in ~100 ns: un fronte lento, normale per un ingresso statico.
 */
public final class DipSwitchLeds
{
    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Stimulus[] hands;
        final Transceiver u1;
        final LedBar d1;
        final Bus in;

        Circuit()
        {
            sim = new Simulation("dipswitch-leds", 1_000);   // 1 ns

            // alimentatore da banco: 1 mohm, più rigido dei contatti chiusi (50 mohm), come nella realtà
            Supply vcc = sim.add(new Supply("VCC", 5.0, 0.001));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 200e-12);
            Wire ground = new Wire("GND", 200e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            hands = new Stimulus[] {
                    new Stimulus(0, 0x00),
                    new Stimulus(1_000_000, 0x0F),
                    new Stimulus(2_000_000, 0xA5),
                    new Stimulus(3_000_000, 0xFF),
            };
            DipSwitch sw1 = sim.add(new DipSwitch("SW1", 8, hands));
            BussedNetwork rn1 = sim.add(new BussedNetwork("RN1", 8, 10_000));
            u1 = sim.add(new Transceiver("U1", Families.HC, 8, 9_000, 15_000, 12_000));   // 74HC245
            IsolatedNetwork rn2 = sim.add(new IsolatedNetwork("RN2", 8, 470));
            d1 = sim.add(new LedBar("D1", 8));

            in = Bus.of("IN", 8, 10e-12);
            Bus out = Bus.of("OUT", 8, 10e-12);
            Bus leds = Bus.of("LED", 8, 5e-12);
            rn1.r().connect(in);
            rn1.common().connect(rail);
            sw1.a().connect(in);
            sw1.b().connect(ground);
            u1.vdd().connect(rail);
            u1.gnd().connect(ground);
            u1.dir().connect(rail);            // da A verso B
            u1.oe().connect(ground);           // sempre abilitato
            u1.a().connect(in);
            u1.b().connect(out);
            rn2.a().connect(out);
            rn2.b().connect(leds);
            d1.a().connect(leds);
            d1.k().connect(ground);

            sim.log().echoTo(System.out);
            sim.add(new RatingSentinel(sim.log(), 20_000));
            sim.vcd("bench")
                    .add(new PortReadSignal(u1.a())).add(new PortDriveSignal(u1.b()))
                    .add(new VoltageSignal(in.get(0))).add(new CurrentSignal(d1.a().get(0)));
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
                    .probe(new PortReadSignal(c.u1.a()), "interruttori (letti dal 245)")
                    .probe(new PortDriveSignal(c.u1.b()), "uscite del 245 (verso i LED)")
                    .probe(new VoltageSignal(c.in.get(0)), "tensione sulla linea IN0")
                    .probe(new CurrentSignal(c.d1.a().get(0)), "corrente nel LED 0");
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
        Stimulus[] hands = circuit.hands;
        LedBar d1 = circuit.d1;

        System.out.println("  levette ON   LED accesi   corrente LED0");
        for (Stimulus s : hands) {
            sim.runUntil(s.timePs() + 900_000);   // poco prima della mossa successiva
            System.out.printf(Locale.ITALIAN, "  %s     %s     %.2f mA%n", bits(s.value()), bits(d1.litMask()),
                    Math.max(0.0, d1.current(0)) * 1000);
        }
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }


    private static String bits(long value)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 7; i >= 0; i--) sb.append((value & (1L << i)) != 0 ? '1' : '0');
        return sb.toString();
    }
}
