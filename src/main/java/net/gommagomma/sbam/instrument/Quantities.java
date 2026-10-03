package net.gommagomma.sbam.instrument;

import java.util.Locale;

/**
 * Come si scrivono le grandezze del simulatore, in un posto solo: tempi, tensioni e correnti, parole; e come si
 * legge un tempo scritto. Le usano gli eventi, le sentinelle, i file e la finestra.
 */
public final class Quantities
{
    private Quantities() {}

    private static final String[] TIME_UNITS = { "ps", "ns", "µs", "ms", "s" };

    /** Un tempo con l'unità più comoda: "250 ns", "12,5 µs". */
    public static String time(long ps)
    {
        double v = ps;
        int u = 0;
        while (u < TIME_UNITS.length - 1 && Math.abs(v) >= 1000) {
            v /= 1000;
            u++;
        }
        return number(v) + " " + TIME_UNITS[u];
    }

    /** Un valore analogico con la sua unità: "3,21 V", "12,5 mA". */
    public static String analog(double value, String unit)
    {
        double a = Math.abs(value);
        if (a != 0 && a < 1e-3) return number(value * 1e6) + " µ" + unit;
        if (a != 0 && a < 1)    return number(value * 1e3) + " m" + unit;
        return number(value) + " " + unit;
    }

    /**
     * Una parola in esadecimale, una cifra ogni 4 bit: X dove un bit è indefinito,
     * Z dove tutti i bit della cifra sono rilasciati.
     */
    public static String word(long value, long unknown, long released, int width)
    {
        int digits = Math.max(1, (width + 3) / 4);
        StringBuilder sb = new StringBuilder(digits);
        for (int d = digits - 1; d >= 0; d--) {
            int bits = Math.min(4, width - d * 4);
            long mask = ((1L << bits) - 1) << (d * 4);
            if ((released & mask) == mask) sb.append('Z');
            else if (((unknown | released) & mask) != 0) sb.append('X');
            else sb.append(Character.toUpperCase(Character.forDigit((int) ((value & mask) >>> (d * 4)), 16)));
        }
        return sb.toString();
    }

    /** Una corrente, senza segno (il verso lo dice il messaggio): "12,5 mA". */
    public static String current(double amps)
    {
        return analog(Math.abs(amps), "A");
    }

    /** Legge un tempo scritto con la sua unità: "10us", "250 ns", "1,5 ms". Restituisce i ps. */
    public static long parseTime(String text)
    {
        String t = text.trim().replace(',', '.').replace(" ", "").replace("µ", "u");
        long scale;
        int unit;
        if (t.endsWith("ps"))      { scale = 1L;                 unit = 2; }
        else if (t.endsWith("ns")) { scale = 1_000L;             unit = 2; }
        else if (t.endsWith("us")) { scale = 1_000_000L;         unit = 2; }
        else if (t.endsWith("ms")) { scale = 1_000_000_000L;     unit = 2; }
        else if (t.endsWith("s"))  { scale = 1_000_000_000_000L; unit = 1; }
        else throw new IllegalArgumentException("un tempo vuole l'unità (ps, ns, us, ms, s): '" + text + "'");
        double v;
        try {
            v = Double.parseDouble(t.substring(0, t.length() - unit));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("tempo non valido: '" + text + "'");
        }
        if (!(v > 0)) throw new IllegalArgumentException("il tempo deve essere positivo: '" + text + "'");
        return Math.round(v * scale);
    }

    /** Un numero con quattro cifre significative circa: 1 decimale sopra 100, 2 sopra 10, 3 sotto. */
    private static String number(double v)
    {
        double a = Math.abs(v);
        String s = String.format(Locale.ITALIAN, a >= 100 ? "%.1f" : a >= 10 ? "%.2f" : "%.3f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(",")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
