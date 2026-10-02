package net.gommagomma.sbam.parts.logic;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.physics.Tick;

/**
 * Un transceiver da n bit, bidirezionale, tri-state:
 *  - OE# alto: entrambi i lati rilasciati;
 *  - OE# basso, DIR alto: il lato A pilota il lato B;
 *  - OE# basso, DIR basso: il lato B pilota il lato A.
 * Un controllo in X (durante un fronte) non cambia la decisione. Il 74HC245 è un Transceiver da 8.
 */
public final class Transceiver extends DigitalDevice
{
    private final DigitalPin dir;
    private final DigitalPin oe;
    private final DigitalPort a;
    private final DigitalPort b;
    private final Timing timing;
    private boolean enabled = false;
    private boolean aToB = true;

    /**
     * @param width         numero di bit
     * @param propagationPs ritardo di propagazione: da un ingresso che cambia all'uscita che cambia [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     * @param enablePs      abilitazione: da OE# attivo all'uscita pilotata [ps]
     *                      (sui datasheet: ten, tPZH/tPZL, tOE)
     * @param disablePs     disabilitazione: da OE# inattivo all'uscita in alta impedenza [ps]
     *                      (sui datasheet: tdis, tPHZ/tPLZ, tHZ, tOHZ)
     */
    public Transceiver(String name, Family family, int width, long propagationPs, long enablePs, long disablePs)
    {
        super(name);
        this.timing = new Timing(propagationPs, propagationPs, enablePs, disablePs);
        Power p = power("VCC", "GND", 10e-12);
        dir = input("DIR", p, family.input());
        oe = input("OE", p, family.input());
        a = port("A", p, family, width);
        b = port("B", p, family, width);
    }

    public DigitalPin dir()   { return dir; }
    public DigitalPin oe()    { return oe; }
    public DigitalPort a()    { return a; }
    public DigitalPort b()    { return b; }

    @Override
    protected void logic(Tick tick)
    {
        enabled = oe.isLow(enabled);
        aToB = dir.isHigh(aToB);
        b.repeat(a, enabled && aToB, tick.next(), timing);
        a.repeat(b, enabled && !aToB, tick.next(), timing);
    }
}
