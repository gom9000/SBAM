package net.gommagomma.sbam.parts.logic;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.logic.SynchronousDevice;
import net.gommagomma.sbam.physics.Tick;

/**
 * Un registro da n bit: flip-flop D sul fronte di salita di CP, uscite tri-state con OE#.
 *
 * Sul fronte di CP memorizza D; i bit letti X non cambiano (il valore reale non sarebbe determinato:
 * lo segnala la sentinella di setup e hold). Con OE# basso le uscite mostrano il registro, con OE#
 * alto sono rilasciate; OE# non tocca il contenuto. Il 74HC574 è un Register da 8.
 */
public final class Register extends SynchronousDevice
{
    private final DigitalPort d;
    private final DigitalPort q;
    private final DigitalPin cp;
    private final DigitalPin oe;
    private final Timing timing;
    private long stored = 0;
    private boolean enabled = false;

    /**
     * @param width         numero di bit
     * @param propagationPs ritardo di propagazione: dal fronte di CP all'uscita Q [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     * @param enablePs      abilitazione: da OE# attivo all'uscita pilotata [ps]
     *                      (sui datasheet: ten, tPZH/tPZL, tOE)
     * @param disablePs     disabilitazione: da OE# inattivo all'uscita in alta impedenza [ps]
     *                      (sui datasheet: tdis, tPHZ/tPLZ, tHZ, tOHZ)
     * @param setupPs       setup: quanto prima del fronte di clock D deve essere già stabile [ps]
     *                      (sui datasheet: tsu, tS, tSU)
     * @param holdPs        hold: quanto dopo il fronte di clock D deve restare stabile [ps]
     *                      (sui datasheet: th, tH, tHD)
     */
    public Register(String name, Family family, int width,
                    long propagationPs, long enablePs, long disablePs, long setupPs, long holdPs)
    {
        super(name);
        this.timing = new Timing(propagationPs, propagationPs, enablePs, disablePs);
        Power p = power("VCC", "GND", 10e-12);
        d = port("D", p, family.input(), OutputStage.NONE, width);
        q = port("Q", p, family, width);
        cp = input("CP", p, family.input());
        oe = input("OE", p, family.input());
        sampling("D", cp, Edge.RISING, d, setupPs, holdPs);
    }

    public DigitalPort d()    { return d; }
    public DigitalPort q()    { return q; }
    public DigitalPin cp()    { return cp; }
    public DigitalPin oe()    { return oe; }

    /** Il contenuto del registro. */
    public long stored()      { return stored; }

    @Override
    protected void logic(Tick tick)
    {
        if (cp.edge() == Edge.RISING) {
            long unknown = d.undefined();
            stored = (d.read() & ~unknown) | (stored & unknown);
        }
        enabled = oe.isLow(enabled);
        q.drive(stored, enabled ? q.all() : 0, tick.next(), timing);
    }
}
