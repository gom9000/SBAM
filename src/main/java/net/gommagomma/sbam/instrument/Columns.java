package net.gommagomma.sbam.instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Le colonne di una capture: ogni segnale dichiarato ha la sua, in ordine di dichiarazione. */
final class Columns
{
    private final List<String> names = new ArrayList<>();
    private final Map<Signal, Integer> index = new IdentityHashMap<>();

    /** Una colonna nuova per il segnale; restituisce il suo numero. */
    int add(Signal signal)
    {
        index.put(signal, names.size());
        names.add(signal.fullName());
        return names.size() - 1;
    }

    /** Il numero della colonna del segnale. */
    int of(Signal signal)       { return index.get(signal); }

    int size()                  { return names.size(); }
    List<String> names()        { return Collections.unmodifiableList(names); }
}
