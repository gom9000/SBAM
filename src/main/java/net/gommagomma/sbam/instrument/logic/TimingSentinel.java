package net.gommagomma.sbam.instrument.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.logic.Sampling;
import net.gommagomma.sbam.logic.SynchronousDevice;
import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;
import net.gommagomma.sbam.physics.Tick;

/**
 * Sentinella di setup e hold: per ogni campionamento dichiarato dai device sincroni, verifica che
 * i pin di dato siano stabili (e non X) da almeno tsu prima del fronte di clock, e che non cambino
 * prima di th dopo. Una violazione è un BOING: il dato catturato non è garantito.
 */
public final class TimingSentinel implements Instrument
{
    private final EventLog log;
    private Sampling[] samplings;
    private long[] lastEdgePs;

    public TimingSentinel(EventLog log)
    {
        this.log = log;
    }

    @Override
    public void observe(Tick tick, Engine engine)
    {
        if (samplings == null) collect(engine);
        long t = tick.next();
        for (int k = 0; k < samplings.length; k++) {
            Sampling s = samplings[k];
            if (s.clock().edge() == s.edge()) {
                lastEdgePs[k] = t;
                for (DigitalPin d : s.data()) checkSetup(s, d, t);
            } else if (lastEdgePs[k] >= 0 && t - lastEdgePs[k] < s.holdPs()) {
                for (DigitalPin d : s.data()) checkHold(s, d, t, lastEdgePs[k]);
            }
        }
    }

    private void checkSetup(Sampling s, DigitalPin d, long edgePs)
    {
        long stable = edgePs - d.stableSincePs();
        if (d.level() == Level.X) {
            log.add(edgePs, Severity.BOING, d.toString(), "X sul fronte di " + s.clock() + " (" + s + ")");
        } else if (stable < s.setupPs()) {
            log.add(edgePs, Severity.BOING, d.toString(), "setup violato: stabile da " + Quantities.time(stable)
                    + " sul fronte di " + s.clock() + ", ne servono " + Quantities.time(s.setupPs()) + " (" + s + ")");
        }
    }

    private void checkHold(Sampling s, DigitalPin d, long t, long edgePs)
    {
        if (d.stableSincePs() == t) {
            log.add(t, Severity.BOING, d.toString(), "hold violato: cambiato " + Quantities.time(t - edgePs)
                    + " dopo il fronte di " + s.clock() + ", ne servono " + Quantities.time(s.holdPs()) + " (" + s + ")");
        }
    }

    private void collect(Engine engine)
    {
        List<Sampling> found = new ArrayList<>();
        for (Device dev : engine.devices()) {
            if (dev instanceof SynchronousDevice) found.addAll(((SynchronousDevice) dev).samplings());
        }
        samplings = found.toArray(new Sampling[0]);
        lastEdgePs = new long[samplings.length];
        Arrays.fill(lastEdgePs, -1);
    }
}
