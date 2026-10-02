package net.gommagomma.sbam.instrument;

import java.util.Locale;

/**
 * Un evento del registro.
 *
 * @param timePs   istante [ps]
 * @param severity gravità
 * @param source   di cosa si parla (un pin, un nodo)
 * @param message  descrizione
 */
public final class Event
{
    private final long timePs;
    private final Severity severity;
    private final String source;
    private final String message;

    public Event(long timePs, Severity severity, String source, String message)
    {
        this.timePs = timePs;
        this.severity = severity;
        this.source = source;
        this.message = message;
    }

    public long timePs()       { return timePs; }
    public Severity severity() { return severity; }
    public String source()     { return source; }
    public String message()    { return message; }


    @Override
    public String toString()
    {
        return String.format("[%14s] %-8s %s: %s", formatTime(timePs), severity.sound(), source, message);
    }

    /** "12,34 mA" (il valore assoluto) */
    public static String formatCurrent(double amps)
    {
        return String.format(Locale.ITALIAN, "%.2f mA", Math.abs(amps) * 1000.0);
    }

    /** "1 234,5 ns" */
    public static String formatTime(long ps)
    {
        String s = String.format(Locale.ITALIAN, "%,.1f", ps / 1000.0).replace('.', ' ');
        if (s.endsWith(",0")) s = s.substring(0, s.length() - 2);
        return s + " ns";
    }
}
