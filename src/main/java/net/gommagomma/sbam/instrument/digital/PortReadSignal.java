package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPort;

/** La parola letta da una porta, con le soglie dei suoi pin. */
public final class PortReadSignal extends BusSignal
{
    public PortReadSignal(DigitalPort port)
    {
        super(port, "read");
    }

    @Override public long value()    { return port().read(); }
    @Override public long unknown()  { return port().undefined(); }
    @Override public long released() { return 0; }
}
