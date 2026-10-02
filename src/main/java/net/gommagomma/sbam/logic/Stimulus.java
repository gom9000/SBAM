package net.gommagomma.sbam.logic;

/**
 * Uno stimolo dall'esterno della rete: da questo istante, questo dato.
 * Per esempio la posizione delle levette di un dip switch, o un pulsante premuto (1) o rilasciato (0).
 */
public final class Stimulus
{
    private final long timePs;
    private final long value;

    public Stimulus(long timePs, long value)
    {
        if (timePs < 0) throw new IllegalArgumentException("istante negativo: " + timePs);
        this.timePs = timePs;
        this.value = value;
    }

    public long timePs() { return timePs; }
    public long value()  { return value; }

    @Override
    public String toString() { return "Stimulus(" + timePs + " ps, " + value + ")"; }
}
