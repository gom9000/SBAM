package net.gommagomma.sbam.physics;

/**
 * L'unità di tempo della simulazione.
 *
 * La durata è libera, ma la fisica la usa come tempo vero: deve essere piccola
 * rispetto ai fenomeni che interessano.
 *
 * @param nowPs  istante di inizio del tick [ps]
 * @param stepPs durata del tick [ps]
 */
public final class Tick
{
    private final long nowPs;
    private final long stepPs;

    public Tick(long nowPs, long stepPs)
    {
        if (stepPs <= 0) throw new IllegalArgumentException("la durata del tick deve essere positiva");
        this.nowPs = nowPs;
        this.stepPs = stepPs;
    }

    public long nowPs()  { return nowPs; }
    public long stepPs() { return stepPs; }

    @Override
    public String toString() { return "Tick(" + nowPs + ", " + stepPs + ")"; }

    /** Istante di fine del tick [ps]. */
    public long next() { return nowPs + stepPs; }

    /** Durata del tick [s], per le formule fisiche. */
    public double seconds() { return stepPs * 1e-12; }
}
