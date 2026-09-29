package net.gommagomma.sbam.physics;


/**
 * Caratteristiche elettriche di un ingresso, prese dal datasheet.
 *
 * Soglie:
 *  - CMOS (relative = true): vih e vil sono FRAZIONI della rail del pin (es. 0,7 e 0,3);
 *  - TTL  (relative = false): vih e vil sono tensioni assolute (es. 2,0 V e 0,8 V).
 *
 * Isteresi (Schmitt trigger, hysteresis = true):
 *  vih e vil diventano le soglie VT+ e VT-. Tra le due il pin CONTINUA A LEGGERE
 *  il valore precedente: non esiste zona indefinita, e un fronte lento o rumoroso
 *  produce una sola commutazione.
 *
 * Diodi di protezione (clamp = true):
 *  se la tensione supera la rail del pin di circa 0,6 V (o scende sotto massa di 0,6 V)
 *  il diodo conduce: la corrente passa dalla linea alla rail (o dalla massa alla linea).
 *  Un chip spento può così essere alimentato attraverso i suoi ingressi.
 *
 * Le correnti seguono la convenzione dei datasheet: positive se entrano nel pin.
 *
 * @param name          descrizione (es. "74HC")
 * @param relative      true se vih/vil sono frazioni della rail
 * @param vih           soglia alta, o VT+ con isteresi (frazione o volt)
 * @param vil           soglia bassa, o VT- con isteresi (frazione o volt)
 * @param hysteresis    ingresso a trigger di Schmitt
 * @param clamp         presenza dei diodi di protezione verso rail e massa
 * @param iihMA         corrente d'ingresso a livello alto [mA]
 * @param iilMA         corrente d'ingresso a livello basso [mA]
 * @param capacitancePF capacità d'ingresso [pF]
 */
public record InputSpec(String name, boolean relative, double vih, double vil,
                        boolean hysteresis, boolean clamp,
                        double iihMA, double iilMA,
                        double capacitancePF)
{
    /** Tensione di conduzione dei diodi di protezione [V]. */
    public static final double CLAMP_VF = 0.6;
    /** Resistenza in conduzione dei diodi di protezione [ohm] (valore indicativo). */
    public static final double CLAMP_OHMS = 20.0;

    public double vihVolts(double railVolts) { return relative ? vih * railVolts : vih; }
    public double vilVolts(double railVolts) { return relative ? vil * railVolts : vil; }

    /**
     * Interpreta una tensione con le soglie di questo ingresso.
     * previous serve solo con l'isteresi: è quello che il pin leggeva al passo prima.
     * NaN = linea flottante.
     */
    public Level read(double volts, double railVolts, Level previous)
    {
        if (Double.isNaN(volts)) return Level.Z;
        double hi = vihVolts(railVolts);
        double lo = vilVolts(railVolts);
        if (volts >= hi) return Level.H;
        if (volts <= lo) return Level.L;
        if (!hysteresis) return Level.X;

        // Schmitt: nella fascia centrale si resta dove si era
        if (previous == Level.H || previous == Level.L) return previous;
        return volts >= (hi + lo) / 2.0 ? Level.H : Level.L;   // senza storia: il lato più vicino
    }

    /** Corrente che entra nel pin quando legge quel livello [mA]. */
    public double currentMA(Level level)
    {
        return switch (level) {
            case H -> iihMA;
            case L -> iilMA;
            default -> 0.0;     // zona indefinita o flottante: la trascuriamo
        };
    }
}
