package net.gommagomma.sbam.instrument;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Una capture su file VCD (Value Change Dump), da aprire con GTKWave o simili.
 * Scrive mentre la simulazione procede, solo i cambiamenti: niente resta in memoria.
 *
 * Logici: 0, 1, x, z. Parole: vettori di bit, con x per i bit indefiniti e z per i rilasciati.
 * Analogici: valori reali, quando cambiano di almeno una risoluzione (vedi resolution).
 * Ogni gruppo di segnali (un device, "wires") è uno scope. Il tempo è in ps.
 */
public final class VcdFile implements Capture
{
    private final Writer out;
    private final ChangeFilter filter = new ChangeFilter();
    private final Columns columns = new Columns();
    private final List<String> ids = new ArrayList<>();
    private final List<Integer> widths = new ArrayList<>();
    private final Map<String, StringBuilder> scopes = new LinkedHashMap<>();
    private long lastTimePs = -1;

    public VcdFile(Writer out)
    {
        this.out = out;
    }

    /** Su un file (sovrascritto). */
    public VcdFile(Path file)
    {
        this(open(file));
    }

    /** La risoluzione per un'unità ("V", "A"). Prima di avviare. */
    public VcdFile resolution(String unit, double step)
    {
        filter.resolution(unit, step);
        return this;
    }

    // ------------------------------------------------------------ dichiarazioni

    @Override
    public void declareLogic(Signal signal)
    {
        declare(signal, "wire", 1, null);
    }

    @Override
    public void declareAnalog(Signal signal, String unit)
    {
        declare(signal, "real", 64, unit);
    }

    @Override
    public void declareWord(Signal signal, int width)
    {
        declare(signal, "wire", width, null);
    }

    @Override
    public void begin()
    {
        write("$version SBAM Modeler $end\n");
        write("$timescale 1ps $end\n");
        write("$scope module sbam $end\n");
        for (Map.Entry<String, StringBuilder> s : scopes.entrySet()) {
            write("$scope module " + s.getKey() + " $end\n");
            write(s.getValue().toString());
            write("$upscope $end\n");
        }
        write("$upscope $end\n");
        write("$enddefinitions $end\n");
    }

    // ------------------------------------------------------------ valori

    @Override
    public void logic(Signal signal, long timePs, char value)
    {
        int c = columns.of(signal);
        if (filter.logic(c, value)) change(timePs, bit(value) + ids.get(c) + "\n");
    }

    @Override
    public void analog(Signal signal, long timePs, double value)
    {
        int c = columns.of(signal);
        if (filter.analog(c, value)) change(timePs, String.format(Locale.ROOT, "r%.6g %s%n", value, ids.get(c)));
    }

    @Override
    public void word(Signal signal, long timePs, long value, long unknown, long released)
    {
        int c = columns.of(signal);
        if (!filter.word(c, value, unknown, released)) return;
        StringBuilder sb = new StringBuilder("b");
        for (int i = widths.get(c) - 1; i >= 0; i--) {
            long bit = 1L << i;
            sb.append((released & bit) != 0 ? 'z' : (unknown & bit) != 0 ? 'x' : (value & bit) != 0 ? '1' : '0');
        }
        change(timePs, sb.append(' ').append(ids.get(c)).append('\n').toString());
    }

    @Override
    public void end()
    {
        try {
            out.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------ interni

    private void declare(Signal signal, String type, int width, String unit)
    {
        int c = columns.add(signal);
        filter.column(unit);
        String id = id(c);
        ids.add(id);
        widths.add(width);
        StringBuilder scope = scopes.get(signal.group());
        if (scope == null) {
            scope = new StringBuilder();
            scopes.put(signal.group(), scope);
        }
        scope.append("$var ").append(type).append(' ').append(width).append(' ').append(id).append(' ')
             .append(signal.name().replace('.', '_')).append(" $end\n");
    }

    /** Scrive un cambiamento, preceduto dall'istante se è il primo di questo istante. */
    private void change(long timePs, String line)
    {
        if (timePs != lastTimePs) {
            write("#" + timePs + "\n");
            lastTimePs = timePs;
        }
        write(line);
    }

    private void write(String s)
    {
        try {
            out.write(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Writer open(Path file)
    {
        try {
            return Files.newBufferedWriter(file, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** L, H, X, Z -> 0, 1, x, z. */
    private static char bit(char value)
    {
        switch (value) {
            case 'L': return '0';
            case 'H': return '1';
            case 'Z': return 'z';
            default:  return 'x';
        }
    }

    /** Identificatore VCD: caratteri stampabili da '!' a '~', in base 94. */
    private static String id(int n)
    {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append((char) ('!' + n % 94));
            n /= 94;
        } while (n > 0);
        return sb.toString();
    }
}
