package net.gommagomma.sbam.fixtures;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;
import net.gommagomma.sbam.physics.Tick;

import java.util.ArrayList;
import java.util.List;

/** Strumenti minimi per i test: annotano ciò che succede su un pin. */
public final class Recorders
{
    private Recorders() {}

    /** Gli istanti in cui l'intenzione di un'uscita passa a H. */
    public static final class RisingDrive implements Instrument
    {
        private final DigitalPin pin;
        private final List<Long> times = new ArrayList<>();
        private boolean wasHigh = false;

        public RisingDrive(DigitalPin pin)
        {
            this.pin = pin;
        }

        public List<Long> times() { return times; }

        @Override
        public void observe(Tick tick, Engine engine)
        {
            boolean high = pin.driven() == Drive.H;
            if (high && !wasHigh) times.add(tick.next());
            wasHigh = high;
        }
    }

    /** Gli istanti in cui l'intenzione di un'uscita cambia. */
    public static final class DriveChanges implements Instrument
    {
        private final DigitalPin pin;
        private final List<Long> times = new ArrayList<>();
        private Drive last;

        public DriveChanges(DigitalPin pin)
        {
            this.pin = pin;
            this.last = pin.driven();
        }

        public List<Long> times() { return times; }

        @Override
        public void observe(Tick tick, Engine engine)
        {
            if (pin.driven() != last) {
                times.add(tick.next());
                last = pin.driven();
            }
        }
    }

    /** I fronti letti da un ingresso, con i loro istanti. */
    public static final class Edges implements Instrument
    {
        private final DigitalPin pin;
        private final List<Edge> edges = new ArrayList<>();
        private final List<Long> times = new ArrayList<>();

        public Edges(DigitalPin pin)
        {
            this.pin = pin;
        }

        public List<Edge> edges() { return edges; }
        public List<Long> times() { return times; }

        @Override
        public void observe(Tick tick, Engine engine)
        {
            if (pin.edge() != Edge.NONE) {
                edges.add(pin.edge());
                times.add(pin.edgeTimePs());
            }
        }
    }

    /** Scorretto: prova a far avanzare la simulazione mentre osserva. */
    public static final class Meddler implements Instrument
    {
        @Override
        public void observe(Tick tick, Engine engine)
        {
            engine.step();
        }
    }
}
