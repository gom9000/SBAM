package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPort;

/** La parola che una porta vuole sul bus: i bit a H, e quelli rilasciati. */
public final class PortDriveSignal extends BusSignal
{
    public PortDriveSignal(DigitalPort port)
    {
        super(port, "drive");
    }

    @Override public long value()    { return port().driven(); }
    @Override public long unknown()  { return 0; }
    @Override public long released() { return port().released(); }
}
