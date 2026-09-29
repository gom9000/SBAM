package net.gommagomma.sbam.engine;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/** Registro di quello che la simulazione nota, con l'istante in cui succede. */
public class Log
{
    public record Entry(long timePs, String source, String message)
    {
        @Override
        public String toString()
        {
            return String.format("[%10s] %-6s %s", Log.formatTime(timePs), source, message);
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private boolean echo = true;


    public void add(long timePs, String source, String message)
    {
        Entry e = new Entry(timePs, source, message);
        entries.add(e);
        if (echo) System.out.println(e);
    }

    public void setEcho(boolean echo)  { this.echo = echo; }
    public List<Entry> getEntries()    { return List.copyOf(entries); }

    /** "1 234,5 ns" */
    public static String formatTime(long ps)
    {
        String s = String.format(Locale.ITALIAN, "%,.1f", ps / 1000.0).replace('.', ' ');
        if (s.endsWith(",0")) s = s.substring(0, s.length() - 2);
        return s + " ns";
    }
}
