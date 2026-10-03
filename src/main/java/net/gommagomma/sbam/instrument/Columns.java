package net.gommagomma.sbam.instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le colonne di una capture, una per segnale dichiarato, in ordine di dichiarazione: tutto quello che si sa
 * di una colonna sta qui, in un posto solo. Il nome, il tipo (logico, analogico, parola), l'unità di un
 * analogico, la larghezza di una parola.
 */
final class Columns
{
    private static final int LOGIC = 0, ANALOG = 1, WORD = 2;

    private final List<String> names = new ArrayList<>();
    private final List<Integer> kinds = new ArrayList<>();
    private final List<String> units = new ArrayList<>();
    private final List<Integer> widths = new ArrayList<>();
    private final Map<Signal, Integer> index = new IdentityHashMap<>();

    /** Una colonna logica (L, H, X, Z); restituisce il suo numero. */
    int logic(Signal signal)                 { return add(signal, LOGIC, null, 1); }

    /** Una colonna analogica, nella sua unità ("V", "A"). */
    int analog(Signal signal, String unit)   { return add(signal, ANALOG, unit, 0); }

    /** Una colonna di parole da width bit. */
    int word(Signal signal, int width)       { return add(signal, WORD, null, width); }

    /** Il numero della colonna del segnale. */
    int of(Signal signal)       { return index.get(signal); }

    int size()                  { return names.size(); }
    List<String> names()        { return Collections.unmodifiableList(names); }
    boolean isAnalog(int c)     { return kinds.get(c) == ANALOG; }
    boolean isWord(int c)       { return kinds.get(c) == WORD; }
    /** L'unità di una colonna analogica; null per le altre. */
    String unit(int c)          { return units.get(c); }
    /** I bit di una colonna: 1 per una logica, la larghezza per una parola, 0 per un'analogica. */
    int width(int c)            { return widths.get(c); }

    private int add(Signal signal, int kind, String unit, int width)
    {
        if (index.containsKey(signal)) throw new IllegalArgumentException(signal + ": dichiarato due volte");
        index.put(signal, names.size());
        names.add(signal.fullName());
        kinds.add(kind);
        units.add(unit);
        widths.add(width);
        return names.size() - 1;
    }
}
