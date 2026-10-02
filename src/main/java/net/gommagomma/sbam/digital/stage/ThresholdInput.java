package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Un ingresso con due soglie e il suo carico sulla linea. Le varianti (BufferInput, SchmittInput)
 * differiscono solo in come leggono il livello; elettricamente sono la stessa cosa.
 */
public abstract class ThresholdInput implements InputStage
{
    private final Thresholds thresholds;
    private final Loading loading;

    protected ThresholdInput(Thresholds thresholds, Loading loading)
    {
        this.thresholds = thresholds;
        this.loading = loading;
    }

    public final Thresholds thresholds() { return thresholds; }
    public final Loading loading()       { return loading; }

    @Override
    public final void react(Pin pin, Power power, Voltages trial, Reaction out)
    {
        double g = trial.volts(power.gnd());
        double s = trial.volts(power.vdd()) - g;
        loading.react(pin, power, trial, g + thresholds.low(s), g + thresholds.high(s), out);
    }

    @Override
    public final double capacitance() { return loading.capacitance(); }

    @Override
    public String toString() { return getClass().getSimpleName() + "(" + thresholds + ", " + loading + ")"; }
}
