package net.gommagomma.sbam.instrument;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Una traccia in memoria: una riga per campionamento, una colonna per segnale. Tiene tutto.
 * È una successione di campioni, uno per istante registrato: tra due campioni non c'è nulla.
 *
 * Come numeri: un valore analogico è sé stesso; un logico vale 0 (L), 1 (H) o NaN (X, Z);
 * una parola vale il suo valore, o NaN se ha bit indefiniti o rilasciati.
 */
public final class Trace implements Capture
{
    private final Columns columns = new Columns();
    private long[] times = new long[1024];
    private double[][] values = new double[0][];
    private int size = 0;

    // ------------------------------------------------------------ lettura

    public List<String> columns()               { return columns.names(); }
    public int size()                           { return size; }
    public long time(int sample)                { return times[sample]; }
    public double value(int column, int sample) { return values[column][sample]; }

    /** Scrive la traccia in CSV: prima colonna il tempo in ns, poi i segnali (virgola come separatore). */
    public void writeCsv(Path file) throws IOException
    {
        Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            w.write("t_ns");
            for (String c : columns.names()) w.write("," + c);
            w.write("\n");
            for (int i = 0; i < size; i++) {
                w.write(String.format(Locale.ROOT, "%.3f", times[i] / 1000.0));
                for (double[] col : values) w.write(String.format(Locale.ROOT, ",%.6g", col[i]));
                w.write("\n");
            }
        } finally {
            w.close();
        }
    }

    // ------------------------------------------------------------ Capture

    @Override public void declareLogic(Signal signal)               { columns.logic(signal); }
    @Override public void declareAnalog(Signal signal, String unit) { columns.analog(signal, unit); }
    @Override public void declareWord(Signal signal, int width)     { columns.word(signal, width); }

    @Override
    public void begin()
    {
        values = new double[columns.size()][times.length];
    }

    @Override
    public void logic(Signal signal, long timePs, char value)
    {
        store(signal, timePs, value == 'L' ? 0.0 : value == 'H' ? 1.0 : Double.NaN);
    }

    @Override
    public void analog(Signal signal, long timePs, double value)
    {
        store(signal, timePs, value);
    }

    @Override
    public void word(Signal signal, long timePs, long value, long unknown, long released)
    {
        store(signal, timePs, (unknown | released) == 0 ? value : Double.NaN);
    }

    @Override
    public void end()
    {
    }

    // ------------------------------------------------------------ interni

    private void store(Signal signal, long timePs, double value)
    {
        int r = row(timePs);                  // prima la riga: può ingrandire gli array
        values[columns.of(signal)][r] = value;
    }

    /** La riga dell'istante dato: una nuova se è il primo valore di questo campionamento. */
    private int row(long timePs)
    {
        if (size > 0 && times[size - 1] == timePs) return size - 1;
        if (size == times.length) {
            times = Arrays.copyOf(times, size * 2);
            for (int c = 0; c < values.length; c++) values[c] = Arrays.copyOf(values[c], size * 2);
        }
        times[size] = timePs;
        for (double[] col : values) col[size] = Double.NaN;
        return size++;
    }
}
