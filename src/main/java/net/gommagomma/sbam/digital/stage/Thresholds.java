package net.gommagomma.sbam.digital.stage;

/**
 * Le soglie di un ingresso.
 *
 *  - relative (CMOS): frazioni di VDD, seguono l'alimentazione (es. 0,7 e 0,3 VDD);
 *  - assolute (TTL):  tensioni fisse (es. 2,0 V e 0,8 V).
 * Con un ingresso Schmitt diventano VT+ e VT-.
 */
public interface Thresholds
{
    /** Soglia alta [V], data la tensione di alimentazione del pin. */
    double high(double vdd);

    /** Soglia bassa [V], data la tensione di alimentazione del pin. */
    double low(double vdd);

    /** Soglie CMOS: frazioni di VDD. */
    static Thresholds relative(double high, double low) { return new Relative(high, low); }

    /** Soglie TTL: tensioni fisse [V]. */
    static Thresholds absolute(double high, double low) { return new Absolute(high, low); }

    final class Relative implements Thresholds
    {
        private final double highFraction;
        private final double lowFraction;

        public Relative(double highFraction, double lowFraction)
        {
            if (!(lowFraction < highFraction)) throw new IllegalArgumentException("serve soglia bassa < soglia alta");
            this.highFraction = highFraction;
            this.lowFraction = lowFraction;
        }

        public double highFraction() { return highFraction; }
        public double lowFraction()  { return lowFraction; }

        @Override
        public String toString() { return "Relative(" + highFraction + ", " + lowFraction + ")"; }

        @Override public double high(double vdd) { return highFraction * vdd; }
        @Override public double low(double vdd)  { return lowFraction * vdd; }
    }

    final class Absolute implements Thresholds
    {
        private final double highVolts;
        private final double lowVolts;

        public Absolute(double highVolts, double lowVolts)
        {
            if (!(lowVolts < highVolts)) throw new IllegalArgumentException("serve soglia bassa < soglia alta");
            this.highVolts = highVolts;
            this.lowVolts = lowVolts;
        }

        public double highVolts() { return highVolts; }
        public double lowVolts()  { return lowVolts; }

        @Override
        public String toString() { return "Absolute(" + highVolts + ", " + lowVolts + ")"; }

        @Override public double high(double vdd) { return highVolts; }
        @Override public double low(double vdd)  { return lowVolts; }
    }
}
