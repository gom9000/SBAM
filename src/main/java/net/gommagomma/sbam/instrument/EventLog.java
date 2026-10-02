package net.gommagomma.sbam.instrument;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Il registro degli eventi, scritto dalle sentinelle. */
public final class EventLog
{
    private final List<Event> events = new ArrayList<>();
    private PrintStream echo = null;

    /** Stampa ogni evento anche su uno stream, mentre accade. */
    public EventLog echoTo(PrintStream out) { this.echo = out; return this; }

    public void add(Event e)
    {
        events.add(e);
        if (echo != null) echo.println(e);
    }

    public void add(long timePs, Severity severity, String source, String message)
    {
        add(new Event(timePs, severity, source, message));
    }

    public List<Event> events() { return Collections.unmodifiableList(events); }

    public long count(Severity severity)
    {
        long n = 0;
        for (Event e : events) {
            if (e.severity() == severity) n++;
        }
        return n;
    }

    /** L'evento più grave (a parità, il primo). Null se il registro è vuoto. */
    public Event worst()
    {
        Event worst = null;
        for (Event e : events) {
            if (worst == null || e.severity().compareTo(worst.severity()) > 0) worst = e;
        }
        return worst;
    }
}
