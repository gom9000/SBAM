package net.gommagomma.sbam.instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decide se un valore è un cambiamento rispetto all'ultimo registrato per quella colonna.
 * Per i valori analogici un cambiamento è uno scarto di almeno una risoluzione, che dipende
 * dall'unità (default: 10 mV, 0,1 mA). Il primo valore di ogni colonna è sempre un cambiamento.
 * Lo usano le capture che tengono solo i cambiamenti.
 */
final class ChangeFilter
{
    private final Map<String, Double> resolutions = new HashMap<>();
    private final List<long[]> last = new ArrayList<>();         // per colonna: l'ultimo valore registrato
    private final List<Double> resolution = new ArrayList<>();   // per colonna analogica; 0 per le altre

    ChangeFilter()
    {
        resolutions.put("V", 0.01);
        resolutions.put("A", 1e-4);
    }

    /** La risoluzione per un'unità: scarti più piccoli non sono cambiamenti. Prima di aggiungere colonne. */
    void resolution(String unit, double step)
    {
        resolutions.put(unit, step);
    }

    /** Aggiunge una colonna; per una analogica, con la risoluzione della sua unità. */
    int column(String unit)
    {
        Double r = unit == null ? null : resolutions.get(unit);
        resolution.add(r == null ? 0.0 : r);
        last.add(null);
        return last.size() - 1;
    }

    boolean logic(int column, char value)
    {
        return exact(column, value, 0, 0);
    }

    boolean word(int column, long value, long unknown, long released)
    {
        return exact(column, value, unknown, released);
    }

    boolean analog(int column, double value)
    {
        long[] l = last.get(column);
        if (l != null && Math.abs(value - Double.longBitsToDouble(l[0])) < resolution.get(column)) return false;
        last.set(column, new long[] { Double.doubleToRawLongBits(value) });
        return true;
    }

    private boolean exact(int column, long a, long b, long c)
    {
        long[] l = last.get(column);
        if (l != null && l[0] == a && l[1] == b && l[2] == c) return false;
        last.set(column, new long[] { a, b, c });
        return true;
    }
}
