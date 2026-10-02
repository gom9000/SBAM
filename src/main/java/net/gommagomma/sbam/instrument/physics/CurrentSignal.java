package net.gommagomma.sbam.instrument.physics;

import net.gommagomma.sbam.physics.Pin;

/** La corrente che un pin manda nel suo nodo [A]. */
public final class CurrentSignal extends AnalogSignal
{
    private final Pin pin;

    public CurrentSignal(Pin pin)
    {
        super(pin.device().name(), pin.name() + ".current");
        this.pin = pin;
    }

    public Pin pin()          { return pin; }

    @Override
    public String unit()      { return "A"; }

    @Override
    public double value()     { return pin.current(); }
}
