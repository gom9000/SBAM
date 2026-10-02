package net.gommagomma.sbam.logic;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un device digitale sincrono: cattura dati sui fronti di un clock (registri, contatori, latch).
 * Oltre ai pin dichiara i suoi campionamenti (Sampling), con i tempi di setup e hold del datasheet,
 * così chi osserva può verificare che il circuito li rispetti.
 */
public abstract class SynchronousDevice extends DigitalDevice
{
    private final List<Sampling> samplings = new ArrayList<>();

    protected SynchronousDevice(String name)
    {
        super(name);
    }

    /** I campionamenti dichiarati. */
    public final List<Sampling> samplings() { return Collections.unmodifiableList(samplings); }

    /** Dichiara un campionamento di una porta. Solo in costruzione. */
    protected final Sampling sampling(String what, DigitalPin clock, Edge edge, DigitalPort data, long setupPs, long holdPs)
    {
        return sampling(what, clock, edge, data.items(), setupPs, holdPs);
    }

    /** Dichiara un campionamento di alcuni pin. Solo in costruzione. */
    protected final Sampling sampling(String what, DigitalPin clock, Edge edge, List<DigitalPin> data, long setupPs, long holdPs)
    {
        require(Phase.BUILD);
        Sampling s = new Sampling(name() + "." + what, clock, edge, data, setupPs, holdPs);
        samplings.add(s);
        return s;
    }
}
