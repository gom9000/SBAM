package net.gommagomma.sbam.parts.logic;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.InputStage;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.logic.SynchronousDevice;
import net.gommagomma.sbam.physics.Tick;

import java.util.Arrays;

/**
 * Un contatore binario sincrono da n bit, con caricamento parallelo e azzeramento asincrono:
 *  - MR# basso: azzera subito, senza aspettare il clock;
 *  - sul fronte di salita di CP: con PE# basso carica D, altrimenti con CEP e CET alti conta;
 *  - TC (riporto) è alto con il contatore al massimo e CET alto: serve a mettere più contatori in cascata.
 * Un solo ritardo di propagazione per Q e TC: sul datasheet CP→TC può differire di qualche ns.
 * Il 74HC161 è un Counter da 4.
 */
public final class Counter extends SynchronousDevice
{
    private final DigitalPort d;
    private final DigitalPort q;
    private final DigitalPin cp, mr, pe, cep, cet, tc;
    private final Timing timing;
    private long count = 0;

    /**
     * @param width         numero di bit
     * @param propagationPs ritardo di propagazione: dal fronte di CP (o da MR#) alle uscite Q e TC [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     * @param setupPs       setup: quanto prima del fronte di clock D, PE#, CEP e CET deve essere già stabile [ps]
     *                      (sui datasheet: tsu, tS, tSU)
     * @param holdPs        hold: quanto dopo il fronte di clock D, PE#, CEP e CET deve restare stabile [ps]
     *                      (sui datasheet: th, tH, tHD)
     */
    public Counter(String name, Family family, int width, long propagationPs, long setupPs, long holdPs)
    {
        super(name);
        this.timing = new Timing(propagationPs, propagationPs);
        Power p = power("VCC", "GND", 10e-12);
        d = port("D", p, family.input(), OutputStage.NONE, width);
        q = port("Q", p, InputStage.NONE, family.output(), width);
        cp = input("CP", p, family.input());
        mr = input("MR", p, family.input());
        pe = input("PE", p, family.input());
        cep = input("CEP", p, family.input());
        cet = input("CET", p, family.input());
        tc = output("TC", p, family.output());
        sampling("D", cp, Edge.RISING, d, setupPs, holdPs);
        sampling("controlli", cp, Edge.RISING, Arrays.asList(pe, cep, cet), setupPs, holdPs);
    }

    public DigitalPort d()    { return d; }
    public DigitalPort q()    { return q; }
    public DigitalPin cp()    { return cp; }
    public DigitalPin mr()    { return mr; }
    public DigitalPin pe()    { return pe; }
    public DigitalPin cep()   { return cep; }
    public DigitalPin cet()   { return cet; }
    public DigitalPin tc()    { return tc; }

    /** Il conteggio. */
    public long count()       { return count; }

    @Override
    protected void logic(Tick tick)
    {
        long now = tick.next();
        if (mr.level() == Level.L) {
            count = 0;
        } else if (cp.edge() == Edge.RISING) {
            if (pe.level() == Level.L) count = d.read() & q.all();
            else if (cep.level() == Level.H && cet.level() == Level.H) count = (count + 1) & q.all();
        }
        q.drive(count, q.all(), now, timing);
        tc.drive(count == q.all() && cet.level() == Level.H ? Drive.H : Drive.L, now, timing);
    }
}
