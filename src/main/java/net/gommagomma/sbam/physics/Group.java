package net.gommagomma.sbam.physics;

import java.util.List;

/**
 * Un gruppo ordinato e con nome di oggetti dello stesso tipo: l'elemento 0 è il bit meno significativo.
 * È l'origine comune di Port (pin di un device) e Bus (fili sulla scheda), che si collegano tra loro
 * come si collegano un pin e un filo.
 */
public abstract class Group<T>
{
    private final String name;
    private final List<T> items;

    protected Group(String name, List<T> items)
    {
        if (items.isEmpty()) throw new IllegalArgumentException(name + ": un gruppo vuoto non ha senso");
        this.name = name;
        this.items = List.copyOf(items);
    }

    public final String name()     { return name; }
    public final int width()       { return items.size(); }
    public final T get(int i)      { return items.get(i); }
    public final List<T> items()   { return items; }

    /** Gli elementi da from (compreso) a to (escluso), per costruire una fetta dello stesso tipo. */
    protected final List<T> range(int from, int to)
    {
        if (from < 0 || to > items.size() || from >= to) {
            throw new IllegalArgumentException(name + ": fetta [" + from + ", " + to + ") fuori da una larghezza di " + items.size());
        }
        return items.subList(from, to);
    }

    /** Il nome di una fetta: "D[0..3]". */
    protected final String sliceName(int from, int to)
    {
        return name + "[" + from + ".." + (to - 1) + "]";
    }

    @Override
    public String toString() { return name + "[" + width() + "]"; }
}
