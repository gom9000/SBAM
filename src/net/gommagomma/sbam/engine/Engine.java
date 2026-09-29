package net.gommagomma.sbam.engine;


import net.gommagomma.sbam.physics.Line;
import net.gommagomma.sbam.physics.Rail;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Il motore a passi fissi.
 *
 * Ogni passo:
 *   fase 0 - le alimentazioni si aggiornano (con l'assorbimento del passo precedente)
 *   fase 1 - si eseguono le azioni in scadenza: i pin dichiarano cosa vogliono fare
 *   fase 2 - ogni linea risolve le dichiarazioni ed evolve per la durata del passo
 *   poi il tempo avanza di un passo.
 *
 * Il tempo è in picosecondi (long). Il passo è configurabile (default 1 ns).
 *
 * Per ora le azioni future stanno in un'unica bacheca dell'engine: serve per pilotare
 * le prove a mano. Quando arriveranno i device, ciascuno avrà le sue.
 */
public class Engine
{
    private final long stepPs;
    private long nowPs = 0;

    private final List<Rail> rails = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private final TreeMap<Long, List<Runnable>> actions = new TreeMap<>();
    private final List<Runnable> probes = new ArrayList<>();
    private final Log log = new Log();


    public Engine(long stepPs)
    {
        if (stepPs <= 0) throw new IllegalArgumentException("il passo deve essere positivo");
        this.stepPs = stepPs;
    }

    public Engine()
    {
        this(1_000);   // 1 ns
    }

    public Line add(Line line)
    {
        lines.add(line);
        return line;
    }

    public Rail add(Rail rail)
    {
        rails.add(rail);
        return rail;
    }

    /** Programma un'azione (fase 1) al primo passo che inizia a partire da quell'istante. */
    public void at(long timePs, Runnable action)
    {
        if (timePs < nowPs) throw new IllegalArgumentException("non si può programmare nel passato");
        actions.computeIfAbsent(timePs, k -> new ArrayList<>()).add(action);
    }

    /** Qualcosa da eseguire alla fine di ogni passo (per osservare, stampare, tracciare). */
    public void probe(Runnable probe)
    {
        probes.add(probe);
    }

    /** Esegue un passo completo. */
    public void step()
    {
        // fase 0: le alimentazioni
        for (Rail r : rails) {
            r.update(nowPs, stepPs, log);
        }

        // fase 1: azioni in scadenza entro l'inizio di questo passo
        while (!actions.isEmpty() && actions.firstKey() <= nowPs) {
            Map.Entry<Long, List<Runnable>> e = actions.pollFirstEntry();
            for (Runnable r : e.getValue()) r.run();
        }

        // fase 2: le linee risolvono ed evolvono per un passo
        for (Line l : lines) {
            l.resolve(nowPs, stepPs, log);
        }

        for (Runnable p : probes) p.run();

        nowPs += stepPs;
    }

    public void runUntil(long timePs)
    {
        while (nowPs < timePs) step();
    }

    public long getNowPs()   { return nowPs; }
    public long getStepPs()  { return stepPs; }
    public Log getLog()      { return log; }

    public static long ns(double ns) { return Math.round(ns * 1000.0); }
}
