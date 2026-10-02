package net.gommagomma.sbam.instrument;

import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;
import net.gommagomma.sbam.physics.Tick;

import java.util.Arrays;
import java.util.List;

/**
 * Una sentinella che sorveglia una condizione su un insieme di oggetti (pin, nodi): la condizione
 * inizia, dura, finisce. Si segnala quando dura almeno un tempo minimo (0: subito), e quando finisce,
 * con la durata e il picco di una grandezza. È lo schema di tutte le sentinelle: ognuna dice solo
 * che cosa sorveglia, qual è la condizione e come la racconta.
 */
public abstract class ConditionSentinel<T> implements Instrument
{
    private final EventLog log;
    private final Severity severity;
    private final long minPs;
    private List<T> items;
    private long[] sincePs;
    private boolean[] reported;
    private double[] peak;

    /** @param minPs quanto deve durare la condizione per essere segnalata [ps] */
    protected ConditionSentinel(EventLog log, Severity severity, long minPs)
    {
        this.log = log;
        this.severity = severity;
        this.minPs = minPs;
    }

    /** Che cosa sorvegliare: raccolto al primo tick, a rete compilata. */
    protected abstract List<T> watch(Engine engine);

    /** Vero mentre la condizione dura. */
    protected abstract boolean holds(T item);

    /** La grandezza di cui tenere il picco mentre la condizione dura (0 se non serve). */
    protected double measure(T item)
    {
        return 0.0;
    }

    /** Di chi si parla nell'evento. */
    protected abstract String source(T item);

    /** Il messaggio quando la condizione viene segnalata, dopo che dura da forPs. */
    protected abstract String started(T item, long forPs);

    /** Il messaggio quando la condizione finisce, dopo forPs, con il picco della grandezza. */
    protected abstract String ended(T item, long forPs, double peak);

    @Override
    public final void observe(Tick tick, Engine engine)
    {
        if (items == null) start(engine);
        long t = tick.next();
        for (int k = 0; k < items.size(); k++) {
            T item = items.get(k);
            if (holds(item)) {
                if (sincePs[k] < 0) {
                    sincePs[k] = t;
                    peak[k] = 0.0;
                }
                peak[k] = Math.max(peak[k], measure(item));
                if (!reported[k] && t - sincePs[k] >= minPs) {
                    reported[k] = true;
                    log.add(t, severity, source(item), started(item, t - sincePs[k]));
                }
            } else if (sincePs[k] >= 0) {
                if (reported[k]) log.add(t, severity, source(item), ended(item, t - sincePs[k], peak[k]));
                sincePs[k] = -1;
                reported[k] = false;
            }
        }
    }

    private void start(Engine engine)
    {
        items = watch(engine);
        sincePs = new long[items.size()];
        reported = new boolean[items.size()];
        peak = new double[items.size()];
        Arrays.fill(sincePs, -1);
    }
}
