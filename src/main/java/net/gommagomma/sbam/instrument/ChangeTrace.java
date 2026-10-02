package net.gommagomma.sbam.instrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Una traccia in memoria che tiene solo i cambiamenti: ogni voce dice "da questo istante,
 * questa colonna vale questo". È la memoria di un analizzatore logico.
 * Tra due cambiamenti il valore resta quello dell'ultimo; i valori analogici cambiano
 * quando si spostano di almeno una risoluzione (vedi resolution).
 */
public final class ChangeTrace implements Capture
{
    private final Columns columns = new Columns();
    private final ChangeFilter filter = new ChangeFilter();
    private long[] times = new long[256];
    private int[] cols = new int[256];
    private long[] values = new long[256];      // logico: il carattere; analogico: i bit del double; parola: il valore
    private long[] unknown = new long[256];
    private long[] released = new long[256];
    private int[] previous = new int[256];      // il cambiamento precedente della stessa colonna; -1 se è il primo
    private int size = 0;
    private int[] latest = new int[0];          // per colonna: l'ultimo cambiamento; -1 se non ce n'è
    private final List<String> units = new ArrayList<>();      // per colonna: l'unità, null se non è analogica
    private final List<Integer> widths = new ArrayList<>();    // per colonna: i bit, 0 se è logica o analogica

    /** La risoluzione per un'unità ("V", "A"). Prima di avviare. */
    public ChangeTrace resolution(String unit, double step)
    {
        filter.resolution(unit, step);
        return this;
    }

    // ------------------------------------------------------------ lettura

    public List<String> columns()   { return columns.names(); }
    /** L'unità della colonna se è analogica ("V", "A"); null altrimenti. */
    public String unit(int column)  { return units.get(column); }
    /** I bit della colonna se è una parola; 0 se è logica o analogica. */
    public int width(int column)    { return widths.get(column); }
    public boolean isAnalog(int column) { return units.get(column) != null; }
    public boolean isWord(int column)   { return widths.get(column) > 0; }
    public int size()               { return size; }
    public long time(int change)    { return times[change]; }
    public int column(int change)   { return cols[change]; }

    public char logic(int change)     { return (char) values[change]; }
    public double analog(int change)  { return Double.longBitsToDouble(values[change]); }
    public long word(int change)      { return values[change]; }
    public long unknown(int change)   { return unknown[change]; }
    public long released(int change)  { return released[change]; }

    /** Quanti cambiamenti ha la colonna (compreso il primo valore). */
    public int changes(int column)
    {
        int n = 0;
        for (int i = 0; i < size; i++) {
            if (cols[i] == column) n++;
        }
        return n;
    }

    /** L'ultimo cambiamento della colonna fino all'istante dato; -1 se non ce n'è ancora uno. */
    public int at(int column, long timePs)
    {
        int i = latest[column];
        while (i >= 0 && times[i] > timePs) i = previous[i];
        return i;
    }

    /** Il cambiamento della stessa colonna prima di questo; -1 se è il primo. */
    public int previous(int change)  { return previous[change]; }

    /** Il primo cambiamento (di qualunque colonna) dall'istante dato in poi; size() se non ce n'è. */
    public int first(long timePs)
    {
        int lo = 0, hi = size;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (times[mid] < timePs) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Il valore logico della colonna all'istante dato; '?' se non c'è ancora. */
    public char logicAt(int column, long timePs)
    {
        int i = at(column, timePs);
        return i < 0 ? '?' : logic(i);
    }

    // ------------------------------------------------------------ Capture

    @Override public void declareLogic(Signal signal)               { declare(signal, null, 0); }
    @Override public void declareAnalog(Signal signal, String unit) { declare(signal, unit, 0); }
    @Override public void declareWord(Signal signal, int width)     { declare(signal, null, width); }
    @Override public void begin() { }
    @Override public void end()   { }

    @Override
    public void logic(Signal signal, long timePs, char value)
    {
        int c = columns.of(signal);
        if (filter.logic(c, value)) add(timePs, c, value, 0, 0);
    }

    @Override
    public void analog(Signal signal, long timePs, double value)
    {
        int c = columns.of(signal);
        if (filter.analog(c, value)) add(timePs, c, Double.doubleToRawLongBits(value), 0, 0);
    }

    @Override
    public void word(Signal signal, long timePs, long value, long unknownBits, long releasedBits)
    {
        int c = columns.of(signal);
        if (filter.word(c, value, unknownBits, releasedBits)) add(timePs, c, value, unknownBits, releasedBits);
    }

    // ------------------------------------------------------------ interni

    private void declare(Signal signal, String unit, int width)
    {
        columns.add(signal);
        filter.column(unit);
        units.add(unit);
        widths.add(width);
        latest = Arrays.copyOf(latest, latest.length + 1);
        latest[latest.length - 1] = -1;
    }

    private void add(long timePs, int column, long value, long unknownBits, long releasedBits)
    {
        if (size == times.length) {
            int n = size * 2;
            times = Arrays.copyOf(times, n);
            cols = Arrays.copyOf(cols, n);
            values = Arrays.copyOf(values, n);
            unknown = Arrays.copyOf(unknown, n);
            released = Arrays.copyOf(released, n);
            previous = Arrays.copyOf(previous, n);
        }
        times[size] = timePs;
        cols[size] = column;
        values[size] = value;
        unknown[size] = unknownBits;
        released[size] = releasedBits;
        previous[size] = latest[column];
        latest[column] = size;
        size++;
    }
}
