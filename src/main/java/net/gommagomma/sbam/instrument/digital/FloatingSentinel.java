package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.instrument.ConditionSentinel;
import net.gommagomma.sbam.instrument.Event;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinella di linea flottante: segnala ogni nodo letto da almeno un ingresso digitale
 * quando nessuno lo pilota, e quando torna pilotato.
 *
 * Un ingresso legge comunque qualcosa (la carica rimasta): il livello letto c'è, ma non lo decide nessuno.
 * Su un bus tri-state succede tra un trasferimento e l'altro; è un problema solo se qualcuno usa quel valore.
 */
public final class FloatingSentinel extends ConditionSentinel<Node>
{
    public FloatingSentinel(EventLog log)
    {
        super(log, Severity.BZZT, 0);
    }

    @Override
    protected List<Node> watch(Engine engine)
    {
        List<Node> read = new ArrayList<>();
        for (Node node : engine.nodes()) {
            if (!DigitalPins.inputs(node).isEmpty()) read.add(node);
        }
        return read;
    }

    @Override protected boolean holds(Node node)  { return node.isFloating(); }
    @Override protected String source(Node node)  { return node.name(); }

    @Override
    protected String started(Node node, long forPs)
    {
        StringBuilder sb = new StringBuilder("flottante, letto da");
        for (DigitalPin p : DigitalPins.inputs(node)) sb.append(' ').append(p).append('=').append(p.level());
        return sb.toString();
    }

    @Override
    protected String ended(Node node, long forPs, double peak)
    {
        return "di nuovo pilotato dopo " + Event.formatTime(forPs);
    }
}
