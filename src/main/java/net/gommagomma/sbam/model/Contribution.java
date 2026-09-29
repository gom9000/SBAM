package net.gommagomma.sbam.model;

/**
 * Il comportamento elettrico di una porta in un'ipotesi di calcolo:
 * "tiro il mio nodo verso questa tensione, con questa forza, e in più inietto questa corrente".
 *
 * È l'equivalente di Norton della porta. Corrente che la porta manda nel suo nodo:
 *
 *     I = conductance * (towardVolts - Vnodo) + current
 *
 * Esempi:
 *  - uscita alta di un 74HC:  verso VDD, conduttanza 1/110 ohm, corrente 0
 *  - uscita in alta impedenza: conduttanza 0 (non tira)
 *  - ingresso TTL basso:       conduttanza 0, corrente +0,4 mA (esce dall'ingresso)
 *  - porta di una resistenza:  verso la tensione dell'altra porta, conduttanza 1/R
 *
 * I comportamenti non lineari (diodi, uscite che limitano la corrente) si esprimono
 * linearizzando attorno alla tensione ipotizzata: per questo il motore può chiedere più volte.
 *
 * @param towardVolts tensione verso cui la porta tira [V]
 * @param conductance forza con cui tira [S = 1/ohm], zero se non tira
 * @param current     corrente aggiuntiva immessa nel nodo [A]
 */
public record Contribution(double towardVolts, double conductance, double current)
{
    /** Una porta che non fa nulla (ingresso ideale, uscita in alta impedenza). */
    public static final Contribution NONE = new Contribution(0.0, 0.0, 0.0);

    /** Tira verso una tensione attraverso una resistenza. */
    public static Contribution toward(double volts, double ohms)
    {
        return new Contribution(volts, 1.0 / ohms, 0.0);
    }

    /** Immette solo una corrente. */
    public static Contribution current(double amps)
    {
        return new Contribution(0.0, 0.0, amps);
    }

    /** Corrente che questa porta manderebbe nel suo nodo, alla tensione data [A]. */
    public double currentInto(double volts)
    {
        return conductance * (towardVolts - volts) + current;
    }
}
