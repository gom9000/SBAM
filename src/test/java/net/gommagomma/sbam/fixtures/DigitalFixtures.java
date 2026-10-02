package net.gommagomma.sbam.fixtures;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.InputStage;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Tick;

import java.util.Map;
import java.util.TreeMap;

/** Device digitali minimi per i test. */
public final class DigitalFixtures
{
    private DigitalFixtures() {}

    /** Un'uscita che segue un programma: da un certo istante in poi, una certa intenzione. */
    public static final class Source extends DigitalDevice
    {
        public final Pin vdd;
        public final Pin gnd;
        public final DigitalPin out;
        private final TreeMap<Long, Drive> plan = new TreeMap<>();

        public Source(String name, OutputStage stage)
        {
            super(name);
            Power p = power("VDD", "GND", 1e-12);
            vdd = p.vdd();
            gnd = p.gnd();
            out = output("Y", p, stage);
        }

        public Source at(long ps, Drive d)
        {
            plan.put(ps, d);
            return this;
        }

        @Override
        protected void logic(Tick tick)
        {
            Map.Entry<Long, Drive> e = plan.floorEntry(tick.next());
            if (e != null) out.drive(e.getValue());
        }
    }

    /**
     * Ripete l'ingresso sull'uscita dopo un ritardo fisso [ps], con l'intenzione in attesa del pin:
     * se l'ingresso cambia idea prima della scadenza, vale la nuova decisione.
     */
    public static final class Follower extends DigitalDevice
    {
        public final Pin vdd;
        public final Pin gnd;
        public final DigitalPin in;
        public final DigitalPin out;
        private final long delayPs;

        public Follower(String name, InputStage input, OutputStage output, long delayPs)
        {
            super(name);
            this.delayPs = delayPs;
            Power p = power("VDD", "GND", 1e-12);
            vdd = p.vdd();
            gnd = p.gnd();
            in = input("A", p, input);
            out = output("Y", p, output);
        }

        @Override
        protected void logic(Tick tick)
        {
            if (in.level() == Level.H) out.drive(Drive.H, tick.next() + delayPs);
            if (in.level() == Level.L) out.drive(Drive.L, tick.next() + delayPs);
        }
    }

    /** Un ingresso che legge e basta. */
    public static final class Sink extends DigitalDevice
    {
        public final Pin vdd;
        public final Pin gnd;
        public final DigitalPin in;

        public Sink(String name, InputStage stage)
        {
            super(name);
            Power p = power("VDD", "GND", 1e-12);
            vdd = p.vdd();
            gnd = p.gnd();
            in = input("A", p, stage);
        }

        @Override
        protected void logic(Tick tick) { }
    }

    /** Una porta che segue un programma: da un certo istante, una parola e i bit abilitati. */
    public static final class WordSource extends DigitalDevice
    {
        public final Pin vdd;
        public final Pin gnd;
        public final DigitalPort port;
        private final TreeMap<Long, long[]> plan = new TreeMap<>();

        public WordSource(String name, Family family, int width)
        {
            super(name);
            Power p = power("VDD", "GND", 1e-12);
            vdd = p.vdd();
            gnd = p.gnd();
            port = port("D", p, family, width);
        }

        public WordSource at(long ps, long value, long enabled)
        {
            plan.put(ps, new long[] { value, enabled });
            return this;
        }

        @Override
        protected void logic(Tick tick)
        {
            Map.Entry<Long, long[]> e = plan.floorEntry(tick.next());
            if (e != null) port.drive(e.getValue()[0], e.getValue()[1]);
        }
    }

    /** Una porta che legge e basta. */
    public static final class WordSink extends DigitalDevice
    {
        public final Pin vdd;
        public final Pin gnd;
        public final DigitalPort port;

        public WordSink(String name, InputStage stage, int width)
        {
            super(name);
            Power p = power("VDD", "GND", 1e-12);
            vdd = p.vdd();
            gnd = p.gnd();
            port = port("D", p, stage, OutputStage.NONE, width);
        }

        @Override
        protected void logic(Tick tick) { }
    }
}
