package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Quantities;

/** Come si scrive sui pannelli il valore di un cambiamento di una traccia, secondo il tipo della sua colonna. */
final class Format
{
    private Format() {}

    static String value(ChangeTrace trace, int column, int change)
    {
        if (trace.isAnalog(column)) return Quantities.analog(trace.analog(change), trace.unit(column));
        if (trace.isWord(column)) return Quantities.word(trace.word(change), trace.unknown(change), trace.released(change), trace.width(column));
        return String.valueOf(trace.logic(change));
    }
}
