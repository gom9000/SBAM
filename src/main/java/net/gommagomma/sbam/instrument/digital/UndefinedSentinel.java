package net.gommagomma.sbam.instrument.digital;

import java.util.ArrayList;
import java.util.List;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.instrument.ConditionSentinel;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Node;

/**
 * Sentinella di livello indefinito: segnala ogni ingresso digitale che legge X più a lungo di un tempo dato,
 * e quando torna a leggere un livello valido, con la durata totale.
 *
 * Attraversare la zona indefinita durante un fronte è normale; restarci significa un fronte troppo lento,
 * una linea a metà tra due uscite in scontro, o un livello di una famiglia che l'altra non capisce.
 */
public final class UndefinedSentinel extends ConditionSentinel<DigitalPin>
{
    /** @param minPs quanto si può restare in X senza segnalazione [ps] */
    public UndefinedSentinel(EventLog log, long minPs)
    {
        super(log, Severity.CRASH, minPs);
    }

    @Override
    protected List<DigitalPin> watch(Engine engine)
    {
        List<DigitalPin> inputs = new ArrayList<>();
        for (Node node : engine.nodes()) inputs.addAll(DigitalPins.inputs(node));
        return inputs;
    }

    @Override protected boolean holds(DigitalPin p)  { return p.level() == Level.X; }
    @Override protected String source(DigitalPin p)  { return p.toString(); }

    @Override
    protected String started(DigitalPin p, long forPs)
    {
        return String.format("legge X da %s (%.2f V)", Quantities.time(forPs), p.node().volts());
    }

    @Override
    protected String ended(DigitalPin p, long forPs, double peak)
    {
        return "di nuovo " + p.level() + " dopo " + Quantities.time(forPs) + " in X";
    }
}
