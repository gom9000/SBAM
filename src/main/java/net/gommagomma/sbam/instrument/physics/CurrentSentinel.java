package net.gommagomma.sbam.instrument.physics;

import java.util.ArrayList;
import java.util.List;

import net.gommagomma.sbam.instrument.ConditionSentinel;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Node;
import net.gommagomma.sbam.physics.Pin;

/**
 * Sentinella di sovracorrente (strato fisico): segnala ogni pin la cui corrente supera
 * un limite, e quando rientra, con la durata e il picco.
 *
 * Vede il sintomo, non la causa: per la fisica uno scontro tra due uscite e un carico
 * pesante sono la stessa cosa. Il KABOOM vero (due uscite in disaccordo) lo vede
 * ContentionSentinel, dalle intenzioni dei pin digitali.
 */
public final class CurrentSentinel extends ConditionSentinel<Pin>
{
    private final double limitAmps;

    public CurrentSentinel(EventLog log, double limitAmps)
    {
        this(log, limitAmps, Severity.BZZT);
    }

    public CurrentSentinel(EventLog log, double limitAmps, Severity severity)
    {
        super(log, severity, 0);
        this.limitAmps = limitAmps;
    }

    @Override
    protected List<Pin> watch(Engine engine)
    {
        List<Pin> pins = new ArrayList<>();
        for (Node node : engine.nodes()) pins.addAll(node.pins());
        return pins;
    }

    @Override protected boolean holds(Pin pin)    { return Math.abs(pin.current()) > limitAmps; }
    @Override protected double measure(Pin pin)   { return Math.abs(pin.current()); }
    @Override protected String source(Pin pin)    { return pin.toString(); }

    @Override
    protected String started(Pin pin, long forPs)
    {
        return "corrente " + Quantities.current((pin.current())) + " oltre il limite di " + Quantities.current(limitAmps);
    }

    @Override
    protected String ended(Pin pin, long forPs, double peak)
    {
        return "corrente rientrata dopo " + Quantities.time(forPs) + " (picco " + Quantities.current(peak) + ")";
    }

}
