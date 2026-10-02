package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.instrument.digital.DriveSignal;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.logic.LogicFunction;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.Trace;
import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.parts.logic.Gate;
import net.gommagomma.sbam.parts.passive.Resistor;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Wire;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Oscillatore RC con un inverter 74HC14: R = 10 kohm dall'uscita all'ingresso, C = 1 nF verso massa.
 * Misura il periodo e lo confronta con la formula; scrive le forme d'onda dei primi 40 us
 * in simulations/schmitt-oscillator/waves.csv.
 */
public final class SchmittOscillator
{
    /** Uno strumento che annota gli istanti in cui un'uscita passa a H. */
    private static final class RisingEdges implements Instrument
    {
        private final DigitalPin pin;
        private final List<Long> times = new ArrayList<>();
        private boolean wasHigh = false;

        private RisingEdges(DigitalPin pin)
        {
            this.pin = pin;
        }

        private List<Long> times() { return times; }

        @Override
        public void observe(Tick tick, Engine engine)
        {
            boolean high = pin.driven() == Drive.H;
            if (high && !wasHigh) times.add(tick.next());
            wasHigh = high;
        }
    }

    private static final double R = 10_000, C = 1e-9;

    /** Il circuito, costruito da capo ogni volta: per la misura da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Supply vcc;
        final Gate u1;
        final Wire in, out;

        Circuit()
        {
            sim = new Simulation("schmitt-oscillator", 10_000);   // 10 ns

            vcc = sim.add(new Supply("VCC", 5.0, 0.1));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 100e-12);
            Wire ground = new Wire("GND", 100e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            u1 = sim.add(new Gate("U1", Families.HC_SCHMITT_GATE, LogicFunction.NOT, 1, 13_000));   // 74HC14
            Resistor rf = sim.add(new Resistor("RF", R));
            in = new Wire("IN", C);                     // il condensatore è la capacità del filo
            out = new Wire("OUT", 10e-12);
            u1.vdd().connect(rail);
            u1.gnd().connect(ground);
            u1.in(0).connect(in);
            u1.y().connect(out);
            rf.a().connect(out);
            rf.b().connect(in);
        }
    }

    /** Per la finestra: il circuito e le sonde, con i nomi da mostrare. */
    private static final class Live implements Setup
    {
        @Override
        public Simulation build(Probes probes)
        {
            Circuit c = new Circuit();
            probes.probe(new DriveSignal(c.u1.y()), "uscita della porta (Y)")
                    .probe(new LevelSignal(c.u1.in(0)), "ingresso letto dalla porta (A)")
                    .probe(new VoltageSignal(c.in), "tensione sul condensatore")
                    .probe(new VoltageSignal(c.out), "tensione d'uscita")
                    .probe(new CurrentSignal(c.vcc.out()), "corrente dall'alimentazione");
            return c.sim;
        }
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length > 0 && args[0].equals("--live")) {              // a banco: la finestra della simulazione
            new SimulationWindow(new Live()).show();
            return;
        }
        double r = R, c = C;
        Circuit circuit = new Circuit();
        Simulation sim = circuit.sim;
        Gate u1 = circuit.u1;
        Wire in = circuit.in, out = circuit.out;
        Supply vcc = circuit.vcc;

        RisingEdges rises = sim.add(new RisingEdges(u1.y()));
        Trace waves = new Trace();
        Recorder recorder = sim.add(new Recorder(waves)
                .add(new VoltageSignal(in)).add(new VoltageSignal(out)).add(new CurrentSignal(vcc.out())));

        sim.runUntil(40_000_000);                        // 40 us registrati
        waves.writeCsv(sim.file("waves.csv"));
        recorder.close();                                // il resto non serve registrarlo
        sim.runUntil(500_000_000);                       // 500 us in tutto

        List<Long> t = rises.times();
        int n = t.size();
        double period = (t.get(n - 1) - t.get(2)) * 1e-12 / (n - 3);
        double ideal = r * c * (Math.log((5 - 1.65) / (5 - 2.75)) + Math.log(2.75 / 1.65));

        System.out.printf(Locale.ITALIAN, "fronti di salita: %d%n", n);
        System.out.printf(Locale.ITALIAN, "periodo misurato: %.3f us  (%.2f kHz)%n", period * 1e6, 1e-3 / period);
        System.out.printf(Locale.ITALIAN, "formula ideale RC*[ln((VDD-VT-)/(VDD-VT+)) + ln(VT+/VT-)]: %.3f us%n", ideal * 1e6);
        System.out.println("(la differenza viene dalla resistenza d'uscita, dalla capacità del pin d'ingresso e dal ritardo della porta)");
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }
}
