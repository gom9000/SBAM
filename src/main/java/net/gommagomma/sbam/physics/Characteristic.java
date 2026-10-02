package net.gommagomma.sbam.physics;

/**
 * La caratteristica elettrica di un pin, vista dal suo nodo (equivalente di Thévenin):
 * un generatore di tensione con in serie una resistenza, più eventualmente un generatore di corrente.
 *
 * Corrente che il pin manda nel suo nodo, alla tensione V del nodo:
 *
 *     I = (volts - V) / resistance + current
 *
 * Esempi:
 *  - uscita 74HC alta:          volts = VDD, resistance = 110 ohm
 *  - uscita 74HC bassa:         volts = 0,   resistance = 55 ohm
 *  - uscita in alta impedenza:  resistance infinita
 *  - pull-up:                   volts = VDD, resistance = 10 kohm
 *  - ingresso TTL basso:        resistance infinita, current = +0,4 mA (esce dall'ingresso)
 *
 * I comportamenti non lineari (diodi, uscite che limitano la corrente) si esprimono
 * con la retta tangente alla tensione di prova: per questo la rete può chiedere più volte.
 *
 * @param volts      tensione del generatore [V]
 * @param resistance resistenza in serie [ohm]: maggiore di zero, infinita se il pin non pilota
 * @param current    corrente aggiuntiva immessa nel nodo [A]
 */
public final class Characteristic
{
    /** Un pin che non carica la linea: alta impedenza, o ingresso ideale. */
    public static final Characteristic OPEN = new Characteristic(0.0, Double.POSITIVE_INFINITY, 0.0);

    private final double volts;
    private final double resistance;
    private final double current;

    public Characteristic(double volts, double resistance, double current)
    {
        if (resistance <= 0.0) {
            throw new IllegalArgumentException("resistenza in serie non valida: " + resistance
                    + " (deve essere maggiore di zero, eventualmente infinita)");
        }
        this.volts = volts;
        this.resistance = resistance;
        this.current = current;
    }

    public double volts()      { return volts; }
    public double resistance() { return resistance; }
    public double current()    { return current; }

    @Override
    public String toString() { return "Characteristic(" + volts + ", " + resistance + ", " + current + ")"; }

    /** Generatore di tensione con resistenza in serie. */
    public static Characteristic source(double volts, double ohms)
    {
        return new Characteristic(volts, ohms, 0.0);
    }

    /** Solo un generatore di corrente (es. la corrente d'ingresso di un TTL). */
    public static Characteristic currentSource(double amps)
    {
        return new Characteristic(0.0, Double.POSITIVE_INFINITY, amps);
    }

    /** 1 / resistenza [S]: zero se il pin non pilota. */
    public double conductance()
    {
        return 1.0 / resistance;
    }

    /** Corrente che il pin manderebbe nel suo nodo, alla tensione data [A]. */
    public double currentInto(double nodeVolts)
    {
        return (volts - nodeVolts) / resistance + current;
    }
}
