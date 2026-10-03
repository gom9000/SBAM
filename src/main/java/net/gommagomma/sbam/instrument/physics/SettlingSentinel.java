package net.gommagomma.sbam.instrument.physics;

import net.gommagomma.sbam.instrument.ConditionSentinel;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.physics.Engine;

import java.util.Collections;
import java.util.List;

/**
 * La sentinella del motore: segnala quando la rete non si assesta entro la tolleranza in un tick, cioè quando
 * le tensioni di quel tick non sono affidabili (accoppiamenti troppo rigidi, un tick troppo lungo), e quando
 * torna ad assestarsi, con lo scarto più grande.
 */
public final class SettlingSentinel extends ConditionSentinel<Engine>
{
    public SettlingSentinel(EventLog log)
    {
        super(log, Severity.BZZT, 0);
    }

    @Override protected List<Engine> watch(Engine engine)   { return Collections.singletonList(engine); }
    @Override protected boolean holds(Engine engine)         { return !engine.lastSettled(); }
    @Override protected double measure(Engine engine)        { return engine.lastResidual(); }
    @Override protected String source(Engine engine)         { return "rete"; }

    @Override
    protected String started(Engine engine, long forPs)
    {
        return "non si assesta in " + engine.lastIterations() + " tentativi (scarto " + Quantities.analog(engine.lastResidual(), "V")
                + "): le tensioni di questi tick non sono affidabili";
    }

    @Override
    protected String ended(Engine engine, long forPs, double peak)
    {
        return "di nuovo assestata dopo " + Quantities.time(forPs) + " (scarto massimo " + Quantities.analog(peak, "V") + ")";
    }
}
