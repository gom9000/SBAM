package net.gommagomma.sbam.parts.logic;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.InputStage;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.physics.Tick;

/**
 * N buffer tri-state con un'abilitazione comune: con OE# basso le uscite Y ripetono gli ingressi A,
 * con OE# alto sono rilasciate. Mezzo 74HC244 è un Buffer da 4, un 74HC541 un Buffer da 8.
 */
public final class Buffer extends DigitalDevice
{
    private final DigitalPort a;
    private final DigitalPort y;
    private final DigitalPin oe;
    private final Timing timing;
    private boolean enabled = false;

    /**
     * @param width         numero di bit
     * @param propagationPs ritardo di propagazione: da un ingresso che cambia all'uscita che cambia [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     * @param enablePs      abilitazione: da OE# attivo all'uscita pilotata [ps]
     *                      (sui datasheet: ten, tPZH/tPZL, tOE)
     * @param disablePs     disabilitazione: da OE# inattivo all'uscita in alta impedenza [ps]
     *                      (sui datasheet: tdis, tPHZ/tPLZ, tHZ, tOHZ)
     */
    public Buffer(String name, Family family, int width, long propagationPs, long enablePs, long disablePs)
    {
        super(name);
        this.timing = new Timing(propagationPs, propagationPs, enablePs, disablePs);
        Power p = power("VCC", "GND", 10e-12);
        a = port("A", p, family.input(), OutputStage.NONE, width);
        y = port("Y", p, InputStage.NONE, family.output(), width);
        oe = input("OE", p, family.input());
    }

    public DigitalPort a()    { return a; }
    public DigitalPort y()    { return y; }
    public DigitalPin oe()    { return oe; }

    @Override
    protected void logic(Tick tick)
    {
        enabled = oe.isLow(enabled);
        y.repeat(a, enabled, tick.next(), timing);
    }
}
