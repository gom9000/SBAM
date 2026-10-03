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
 * Sentinella di specifica: segnala ogni uscita digitale che eroga o assorbe più della corrente
 * alla quale il suo stadio garantisce ancora i livelli (IOH / IOL del datasheet), e quando rientra.
 *
 * IOH e IOL sono correnti statiche: durante un fronte la corrente che carica la capacità della linea
 * le supera sempre, ed è normale. Per questo si segnala solo un sovraccarico che dura più di un tempo dato.
 */
public final class RatingSentinel extends ConditionSentinel<DigitalPin>
{
    /** @param minPs quanto può durare un sovraccarico senza segnalazione [ps] (i fronti durano pochi ns) */
    public RatingSentinel(EventLog log, long minPs)
    {
        super(log, Severity.BZZT, minPs);
    }

    @Override
    protected List<DigitalPin> watch(Engine engine)
    {
        List<DigitalPin> outputs = new ArrayList<>();
        for (Node node : engine.nodes()) outputs.addAll(DigitalPins.outputs(node));
        return outputs;
    }

    @Override
    protected boolean holds(DigitalPin p)
    {
        return p.driven() != Drive.Z && Math.abs(p.current()) > p.output().ratedCurrent(p.driven());
    }

    @Override protected double measure(DigitalPin p) { return Math.abs(p.current()); }
    @Override protected String source(DigitalPin p)  { return p.toString(); }

    @Override
    protected String started(DigitalPin p, long forPs)
    {
        return "uscita " + p.driven() + " a " + Quantities.current((p.current())) + " da " + Quantities.time(forPs)
                + ", oltre i " + Quantities.current(p.output().ratedCurrent(p.driven())) + " garantiti: il livello non è più garantito";
    }

    @Override
    protected String ended(DigitalPin p, long forPs, double peak)
    {
        return "rientrata nella specifica dopo " + Quantities.time(forPs) + " (picco " + Quantities.current(peak) + ")";
    }

}
