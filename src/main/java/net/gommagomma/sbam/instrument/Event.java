package net.gommagomma.sbam.instrument;

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
        return String.format("[%14s] %-8s %s: %s", Quantities.time(timePs), severity.sound(), source, message);
    }
}
