package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.physics.CurrentSentinel;
import net.gommagomma.sbam.parts.passive.Diode;
import net.gommagomma.sbam.parts.passive.Resistor;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;

import java.util.Locale;

/**
 * Il LED di una barra di monitoraggio del bus: 5 V, resistenza, LED rosso (~2 V), massa.
 * Con resistenze diverse si vede la corrente nel LED, e una sentinella segnala
 * quando supera i 20 mA tipici di un LED (e quasi sempre il limite del pin che lo pilota).
 */
public final class LedResistor
{
    /** Per la finestra: lo stesso circuito con una resistenza sola, e le sonde con i nomi da mostrare. */
    private static final class Live implements Setup
    {
        private final double ohms;

        Live(double ohms) { this.ohms = ohms; }

        @Override
        public Simulation build(Probes probes)
        {
            Simulation sim = new Simulation("led-resistor", 1_000);   // 1 ns
            Supply vcc = sim.add(new Supply("VCC", 5.0, 0.1));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.1));
            Resistor r = sim.add(new Resistor("R", ohms));
            Diode led = sim.add(new Diode("LED", 2.0, 15.0));
            Wire rail = new Wire("+5V", 10e-12);
            Wire anode = new Wire("A", 10e-12);
            Wire ground = new Wire("GND", 10e-12);
            vcc.out().connect(rail);
            r.a().connect(rail);
            r.b().connect(anode);
            led.anode().connect(anode);
            led.cathode().connect(ground);
            gnd.out().connect(ground);
            sim.add(new CurrentSentinel(sim.log(), 20e-3));      // qui anche la corrente di spunto dell'accensione
            probes.probe(new VoltageSignal(anode), "tensione sull'anodo")
                    .probe(new CurrentSignal(r.b()), "corrente nel LED");
            return sim;
        }
    }

    /** Con --live [ohm] apre la finestra con una resistenza sola (default 330 ohm). */
    public static void main(String[] args)
    {
        if (args.length > 0 && args[0].equals("--live")) {              // a banco: la finestra della simulazione
            new SimulationWindow(new Live(args.length > 1 ? Double.parseDouble(args[1]) : 330)).show();
            return;
        }
        for (double ohms : new double[] { 1_000, 330, 150, 100 }) {
            Engine engine = new Engine(1_000);   // 1 ns

            Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
            Supply gnd = engine.add(new Supply("GND", 0.0, 0.1));
            Resistor r = engine.add(new Resistor("R", ohms));
            Diode led = engine.add(new Diode("LED", 2.0, 15.0));

            Wire rail = new Wire("+5V", 10e-12);
            Wire anode = new Wire("A", 10e-12);
            Wire ground = new Wire("GND", 10e-12);
            vcc.out().connect(rail);
            r.a().connect(rail);
            r.b().connect(anode);
            led.anode().connect(anode);
            led.cathode().connect(ground);
            gnd.out().connect(ground);

            // all'accensione tutto parte da 0 V: nel primo tick l'alimentatore carica di colpo
            // la capacità del filo +5V (corrente di spunto). La sentinella entra dopo l'accensione,
            // per guardare solo il regime.
            engine.runUntil(100_000);
            EventLog log = new EventLog();
            engine.add(new CurrentSentinel(log, 20e-3));
            engine.runUntil(1_000_000);          // 1 us, abbondantemente a regime

            System.out.printf(Locale.ITALIAN, "R = %4.0f ohm: anodo %.2f V, corrente nel LED %5.2f mA%s%n",
                    ohms, anode.volts(), r.b().current() * 1000,
                    log.events().isEmpty() ? "" : "   <- " + log.events().get(0).severity().sound() + " oltre 20 mA");
        }
    }
}
