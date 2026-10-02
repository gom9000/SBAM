package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/** La metà bassa di uno stadio d'uscita: una resistenza tra il pin e il pin GND del device. */
public final class LowSide
{
    private final double ohms;

    public LowSide(double ohms)
    {
        this.ohms = ohms;
    }

    public double ohms() { return ohms; }

    @Override
    public String toString() { return "LowSide(" + ohms + ")"; }

    /** Dal datasheet: VOL garantita a una corrente IOL. */
    public static LowSide of(double vol, double iolAmps)
    {
        return new LowSide(vol / iolAmps);
    }

    /** Una resistenza tra il pin e GND: la corrente assorbita rientra nel filo di massa. */
    void react(Pin pin, Pin gnd, Voltages trial, Reaction out)
    {
        out.set(pin, trial.volts(gnd), ohms, 0.0);
        out.set(gnd, trial.volts(pin), ohms, 0.0);
    }
}
