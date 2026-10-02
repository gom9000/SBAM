package net.gommagomma.sbam.fixtures;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/** Device minimi per i test: non fanno parte della libreria. */
public final class Fixtures
{
    private Fixtures() {}

    /** Pilota il pin con una tensione attraverso una resistenza fino a releasePs, poi lo lascia aperto. */
    public static final class Switch extends Device
    {
        public final Pin out;
        private final double volts;
        private final double ohms;
        private final long releasePs;
        private boolean driving = true;

        public Switch(String name, double volts, double ohms, long releasePs)
        {
            super(name);
            this.volts = volts;
            this.ohms = ohms;
            this.releasePs = releasePs;
            this.out = pin("OUT", 5e-12);
        }

        @Override
        protected void react(Tick tick, Voltages trial, Reaction r)
        {
            if (driving) r.set(out, volts, ohms, 0.0);
        }

        @Override
        protected void update(Tick tick, Voltages settled)
        {
            if (tick.next() >= releasePs) driving = false;
        }
    }

    /** Scorretto: cambia stato dentro react. */
    public static final class Impure extends Device
    {
        public final Pin out;
        private int calls = 0;

        public Impure()
        {
            super("IMPURE");
            out = pin("OUT", 5e-12);
        }

        @Override
        protected void react(Tick tick, Voltages trial, Reaction r)
        {
            calls++;
            r.set(out, calls % 2 == 0 ? 5.0 : 0.0, 100.0, 0.0);
        }

        @Override
        protected void update(Tick tick, Voltages settled) { }
    }

    /** Scorretto: dichiara il pin di un altro device. */
    public static final class Thief extends Device
    {
        public final Pin own;
        public Pin stolen;

        public Thief()
        {
            super("THIEF");
            own = pin("OWN", 5e-12);
        }

        @Override
        protected void react(Tick tick, Voltages trial, Reaction r)
        {
            r.set(stolen, 5.0, 100.0, 0.0);
        }

        @Override
        protected void update(Tick tick, Voltages settled) { }
    }

    /** Dichiara due volte lo stesso pin: due generatori in parallelo. */
    public static final class Twin extends Device
    {
        public final Pin out;

        public Twin()
        {
            super("TWIN");
            out = pin("OUT", 5e-12);
        }

        @Override
        protected void react(Tick tick, Voltages trial, Reaction r)
        {
            r.set(out, 5.0, 100.0, 0.0);
            r.set(out, 0.0, 100.0, 0.0);
        }

        @Override
        protected void update(Tick tick, Voltages settled) { }
    }

    /**
     * Uscita digitale grezza che commuta con un periodo: alta verso il proprio VDD (110 ohm),
     * bassa verso massa (55 ohm). Il livello si decide in update.
     */
    public static final class Toggler extends Device
    {
        public final Pin out;
        public final Pin vdd;
        private final long halfPeriodPs;
        private boolean high = false;

        public Toggler(String name, long halfPeriodPs)
        {
            super(name);
            this.halfPeriodPs = halfPeriodPs;
            out = pin("OUT", 5e-12);
            vdd = pin("VDD", 5e-12);
        }

        @Override
        protected void react(Tick tick, Voltages trial, Reaction r)
        {
            if (high) r.set(out, trial.volts(vdd), 110.0, 0.0);
            else      r.set(out, 0.0, 55.0, 0.0);
        }

        @Override
        protected void update(Tick tick, Voltages settled)
        {
            high = (tick.next() / halfPeriodPs) % 2 == 1;
        }
    }

    /** Un carico: k ingressi con una piccola corrente di perdita. */
    public static final class Load extends Device
    {
        public final Pin[] in;

        public Load(String name, int k)
        {
            super(name);
            in = new Pin[k];
            for (int i = 0; i < k; i++) in[i] = pin("I" + i, 4e-12);
        }

        @Override
        protected void react(Tick tick, Voltages trial, Reaction r)
        {
            for (Pin p : in) r.set(p, 0.0, Double.POSITIVE_INFINITY, 1e-6);
        }

        @Override
        protected void update(Tick tick, Voltages settled) { }
    }
}
