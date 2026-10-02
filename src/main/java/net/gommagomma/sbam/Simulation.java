package net.gommagomma.sbam;

import net.gommagomma.sbam.instrument.Event;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.instrument.VcdFile;
import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Una simulazione con un nome: possiede la rete (Engine) e una cartella, simulations/<nome>/,
 * dove finiscono tutti i file che produce. Una nuova esecuzione con lo stesso nome li sovrascrive.
 *
 * Le registrazioni su file le crea la simulazione (vcd, record), che le chiude in close();
 * alla chiusura scrive anche il registro degli eventi (events.txt) e un riepilogo (run.txt).
 *
 *     Simulation sim = new Simulation("contention", 10_000);
 *     ... sim.add(device) ...
 *     sim.vcd("bus").add(new DriveSignal(u1.y())).add(new VoltageSignal(line));
 *     sim.add(new ContentionSentinel(sim.log()));
 *     sim.runUntil(20_000_000);
 *     sim.close();
 *
 * L'engine resta usabile da solo (test, prove senza file): engine() lo espone per tutto il resto.
 */
public final class Simulation
{
    /** La cartella di tutte le simulazioni, relativa alla cartella da cui si lancia. */
    public static final Path SIMULATIONS = Path.of("simulations");

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]+");

    private final String name;
    private final Engine engine;
    private final Path dir;
    private final EventLog log = new EventLog();
    private final List<Recorder> recorders = new ArrayList<>();
    private long wallNanos = 0;
    private boolean closed = false;

    public Simulation(String name, long tickPs)
    {
        this(name, tickPs, SIMULATIONS);
    }

    /** Con una cartella base diversa da simulations/ (per esempio nei test). */
    public Simulation(String name, long tickPs, Path base)
    {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("nome di simulazione non valido come cartella: '" + name + "'");
        }
        this.name = name;
        this.engine = new Engine(tickPs);
        this.dir = base.resolve(name);
    }

    public String name()    { return name; }
    public Engine engine()  { return engine; }
    public Path dir()       { return dir; }

    /** Il registro degli eventi della simulazione, da dare alle sentinelle; finisce in events.txt. */
    public EventLog log()   { return log; }

    // ------------------------------------------------------------ circuito e tempo

    public <D extends Device> D add(D device)              { return engine.add(device); }

    public <I extends Instrument> I add(I instrument)      { return engine.add(instrument); }

    public void step()
    {
        long t0 = System.nanoTime();
        engine.step();
        wallNanos += System.nanoTime() - t0;
    }

    public void runUntil(long timePs)
    {
        long t0 = System.nanoTime();
        engine.runUntil(timePs);
        wallNanos += System.nanoTime() - t0;
    }

    // ------------------------------------------------------------ file

    /** Il percorso di un file della simulazione (la cartella viene creata se manca). */
    public Path file(String fileName)
    {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir.resolve(fileName);
    }

    /**
     * Una registrazione su VCD, <nome>.vcd: già aggiunta alla rete; si aggiungono i segnali,
     * e il file si chiude con la simulazione.
     */
    public Recorder vcd(String fileName)
    {
        return record(new Recorder(new VcdFile(file(fileName + ".vcd"))));
    }

    /** Aggiunge una registrazione alla rete, e la chiude con la simulazione. */
    public Recorder record(Recorder recorder)
    {
        engine.add(recorder);
        recorders.add(recorder);
        return recorder;
    }

    // ------------------------------------------------------------ fine

    /** Chiude i file degli strumenti, scrive events.txt e run.txt. Chiamarlo più volte non fa nulla. */
    public void close()
    {
        if (closed) return;
        closed = true;
        try {
            for (Recorder r : recorders) r.close();
            writeEvents();
            writeSummary();
        } catch (IOException e) {
            throw new UncheckedIOException(name + ": errore nel chiudere i file", e);
        }
    }

    private void writeEvents() throws IOException
    {
        Writer w = Files.newBufferedWriter(file("events.txt"), StandardCharsets.UTF_8);
        try {
            for (Event e : log.events()) w.write(e + "\n");
        } finally {
            w.close();
        }
    }

    private void writeSummary() throws IOException
    {
        long ps = engine.nowPs();
        double wall = wallNanos * 1e-9;
        Writer w = Files.newBufferedWriter(file("run.txt"), StandardCharsets.UTF_8);
        try {
            line(w, "simulazione", name);
            line(w, "eseguita", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            line(w, "tick", Event.formatTime(engine.stepPs()));
            line(w, "tempo simulato", Event.formatTime(ps) + " (" + ps / engine.stepPs() + " tick)");
            line(w, "tempo reale", String.format(Locale.ITALIAN, "%.3f s", wall));
            line(w, "device", String.valueOf(engine.devices().size()));
            line(w, "tick non assestati", String.valueOf(engine.unsettledTicks()));
            line(w, "eventi", log.events().size() + eventCounts());
            for (String warning : engine.warnings()) line(w, "avviso", warning);
        } finally {
            w.close();
        }
    }

    private static void line(Writer w, String label, String value) throws IOException
    {
        w.write(String.format("%-20s%s%n", label, value));
    }

    private String eventCounts()
    {
        StringBuilder sb = new StringBuilder();
        for (Severity s : Severity.values()) {
            long n = log.count(s);
            if (n > 0) sb.append(sb.length() == 0 ? " (" : ", ").append(s.sound()).append(' ').append(n);
        }
        return sb.length() == 0 ? "" : sb.append(')').toString();
    }
}
