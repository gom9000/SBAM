package net.gommagomma.sbam.hardware.passive;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Diodo ideale a tratti: conduce quando la tensione anodo-catodo supera la tensione di soglia,
 * con una piccola resistenza in serie; altrimenti è aperto.
 *
 * Visto dall'anodo, quando conduce: generatore pari a (V catodo + soglia), con in serie la resistenza.
 * Visto dal catodo: generatore pari a (V anodo - soglia), con in serie la resistenza.
 *
 * È un device non lineare: la caratteristica dipende dalle tensioni di prova.
 */
public final class Diode extends Device
{
    private final Pin anode;
    private final Pin cathode;
    private final double forwardVolts;
    private final double onOhms;

    /**
     * @param forwardVolts tensione di soglia [V] (0,6 per un diodo al silicio, ~2 per un LED rosso)
     * @param onOhms       resistenza in conduzione [ohm]
     */
    public Diode(String name, double forwardVolts, double onOhms)
    {
        super(name);
        this.forwardVolts = forwardVolts;
        this.onOhms = onOhms;
        this.anode = pin("A", 0.0);
        this.cathode = pin("K", 0.0);
    }

    public Pin anode()   { return anode; }
    public Pin cathode() { return cathode; }

    @Override
    protected void react(Tick tick, Voltages trial, Reaction r)
    {
        double va = trial.volts(anode);
        double vk = trial.volts(cathode);
        if (va - vk > forwardVolts) {
            r.set(anode, vk + forwardVolts, onOhms, 0.0);
            r.set(cathode, va - forwardVolts, onOhms, 0.0);
        }
    }

    @Override
    protected void update(Tick tick, Voltages settled)
    {
    }
}
