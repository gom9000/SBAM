package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/**
 * La metà alta di uno stadio d'uscita: come il pin viene tirato verso VDD.
 * È sempre un ramo tra il pin VDD e il pin d'uscita, così la corrente erogata esce da VDD.
 */
public interface HighSide
{
    void react(Pin pin, Pin vdd, Voltages trial, Reaction out);

    /**
     * CMOS, rail-to-rail: una resistenza tra VDD e il pin, ricavata dal datasheet
     * (VOH garantita a una corrente IOH, alla VDD di specifica).
     */
    static HighSide cmos(double specVdd, double voh, double iohAmps)
    {
        return new Cmos((specVdd - voh) / iohAmps);
    }

    /**
     * TTL: l'uscita alta si ferma sotto VDD (a vuoto vohOpen alla VDD di specifica) e non assorbe
     * corrente se la linea è più alta di così.
     */
    static HighSide ttl(double specVdd, double vohOpen, double voh, double iohAmps)
    {
        return new Ttl(specVdd - vohOpen, (vohOpen - voh) / iohAmps);
    }

    /** Resistenza tra VDD e il pin. */
    final class Cmos implements HighSide
    {
        private final double ohms;

        public Cmos(double ohms)
        {
            this.ohms = ohms;
        }

        public double ohms() { return ohms; }

        @Override
        public String toString() { return "Cmos(" + ohms + ")"; }

        @Override
        public void react(Pin pin, Pin vdd, Voltages trial, Reaction out)
        {
            out.set(pin, trial.volts(vdd), ohms, 0.0);
            out.set(vdd, trial.volts(pin), ohms, 0.0);
        }
    }

    /** Generatore pari a (VDD - drop) con resistenza in serie, che conduce solo verso il pin. */
    final class Ttl implements HighSide
    {
        private final double dropVolts;
        private final double ohms;

        public Ttl(double dropVolts, double ohms)
        {
            this.dropVolts = dropVolts;
            this.ohms = ohms;
        }

        public double dropVolts() { return dropVolts; }
        public double ohms()      { return ohms; }

        @Override
        public String toString() { return "Ttl(" + dropVolts + ", " + ohms + ")"; }

        @Override
        public void react(Pin pin, Pin vdd, Voltages trial, Reaction out)
        {
            double source = trial.volts(vdd) - dropVolts;
            double v = trial.volts(pin);
            if (v < source) {
                out.set(pin, source, ohms, 0.0);
                out.set(vdd, v + dropVolts, ohms, 0.0);
            }
        }
    }
}
