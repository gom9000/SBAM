package net.gommagomma.sbam.instrument.physics;

import net.gommagomma.sbam.instrument.ConditionSentinel;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Engine;

import java.util.List;

/** La sentinella dei guasti: segnala ogni device che si mette in uno stato di guasto (Device.fault). */
public final class FaultSentinel extends ConditionSentinel<Device>
{
    public FaultSentinel(EventLog log)
    {
        super(log, Severity.CRASH, 0);
    }

    @Override protected List<Device> watch(Engine engine)   { return engine.devices(); }
    @Override protected boolean holds(Device device)         { return device.fault() != null; }
    @Override protected String source(Device device)         { return device.name(); }
    @Override protected String started(Device device, long forPs)              { return device.fault(); }
    @Override protected String ended(Device device, long forPs, double peak)   { return "guasto finito dopo " + Quantities.time(forPs); }
}
