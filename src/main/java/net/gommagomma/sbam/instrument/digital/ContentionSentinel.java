package net.gommagomma.sbam.instrument.digital;

import java.util.ArrayList;
import java.util.List;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.instrument.ConditionSentinel;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Node;

/**
 * Sentinella di scontro: segnala ogni nodo su cui un'uscita vuole H e un'altra vuole L,
 * e quando lo scontro finisce, con la durata e la corrente di picco.
 *
 * Guarda le intenzioni, non le correnti: è la causa, dove CurrentSentinel vede il sintomo.
 */
public final class ContentionSentinel extends ConditionSentinel<ContentionSentinel.Line>
{
    /** Un nodo con almeno due uscite digitali. */
    static final class Line
    {
        private final Node node;
        private final List<DigitalPin> drivers;

        private Line(Node node, List<DigitalPin> drivers)
        {
            this.node = node;
            this.drivers = drivers;
        }
    }

    public ContentionSentinel(EventLog log)
    {
        super(log, Severity.KABOOM, 0);
    }

    @Override
    protected List<Line> watch(Engine engine)
    {
        List<Line> lines = new ArrayList<>();
        for (Node node : engine.nodes()) {
            List<DigitalPin> outs = DigitalPins.outputs(node);
            if (outs.size() >= 2) lines.add(new Line(node, outs));
        }
        return lines;
    }

    @Override
    protected boolean holds(Line line)
    {
        boolean high = false, low = false;
        for (DigitalPin p : line.drivers) {
            high |= p.driven() == Drive.H;
            low |= p.driven() == Drive.L;
        }
        return high && low;
    }

    @Override
    protected double measure(Line line)
    {
        double max = 0.0;
        for (DigitalPin p : line.drivers) max = Math.max(max, Math.abs(p.current()));
        return max;
    }

    @Override
    protected String source(Line line) { return line.node.name(); }

    @Override
    protected String started(Line line, long forPs)
    {
        StringBuilder sb = new StringBuilder("scontro:");
        for (DigitalPin p : line.drivers) {
            if (p.driven() != Drive.Z) sb.append(' ').append(p).append('=').append(p.driven());
        }
        return sb.toString();
    }

    @Override
    protected String ended(Line line, long forPs, double peak)
    {
        return "fine dello scontro dopo " + Quantities.time(forPs) + " (picco " + Quantities.current(peak) + ")";
    }
}
