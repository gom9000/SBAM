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
 * Un decodificatore da n a 2^n: n ingressi di selezione (A), 2^n uscite attive basse (Y), e degli enable,
 * alcuni attivi bassi (G#) e alcuni attivi alti (G). Abilitato (tutti gli enable attivi), è bassa solo
 * l'uscita selezionata; altrimenti sono tutte alte. Come per le porte, non è un chip: un 74HC138 è un
 * Decoder da 3 con un enable alto e due bassi, mezzo 74HC139 è un Decoder da 2 con un enable basso.
 *
 * Se la selezione o un enable leggono X, le uscite mantengono le intenzioni che avevano.
 */
public final class Decoder extends DigitalDevice
{
    private final DigitalPort select;
    private final DigitalPort enablesLow;
    private final DigitalPort enablesHigh;
    private final DigitalPort y;
    private final Timing timing;
    private boolean enabled = false;
    private long selected = 0;

    /**
     * @param selectBits  n, il numero di ingressi di selezione (le uscite sono 2^n)
     * @param enablesLow  quanti enable attivi bassi
     * @param enablesHigh quanti enable attivi alti
     * @param propagationPs ritardo di propagazione: da una selezione o un enable che cambia all'uscita [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     */
    public Decoder(String name, Family family, int selectBits, int enablesLow, int enablesHigh, long propagationPs)
    {
        super(name);
        if (selectBits < 1 || selectBits > 6) throw new IllegalArgumentException(name + ": da 1 a 6 ingressi di selezione");
        if (enablesLow + enablesHigh < 1) throw new IllegalArgumentException(name + ": almeno un enable");
        this.timing = new Timing(propagationPs, propagationPs);
        int outputs = 1 << selectBits;
        Power p = power("VDD", "GND", 5e-12);
        select = port("A", p, family.input(), OutputStage.NONE, selectBits);
        this.enablesLow = enablesLow > 0 ? port("GN", p, family.input(), OutputStage.NONE, enablesLow) : null;
        this.enablesHigh = enablesHigh > 0 ? port("G", p, family.input(), OutputStage.NONE, enablesHigh) : null;
        y = port("Y", p, InputStage.NONE, family.output(), outputs);
        y.drive(y.all());                 // all'accensione: tutte alte
    }

    public DigitalPort select()       { return select; }
    public DigitalPin a(int i)        { return select.get(i); }
    public DigitalPin enableLow(int i){ return enablesLow.get(i); }
    public DigitalPin enableHigh(int i){ return enablesHigh.get(i); }
    public DigitalPort y()            { return y; }
    public DigitalPin y(int i)        { return y.get(i); }

    @Override
    protected void logic(Tick tick)
    {
        enabled = allLow(enablesLow) && allHigh(enablesHigh);
        if (select.undefined() == 0) selected = select.read();
        y.drive(enabled ? y.all() & ~(1L << selected) : y.all(), y.all(), tick.next(), timing);
    }

    /** Vero se tutti gli enable attivi bassi sono bassi (nessuno: vero). Un X conserva la decisione di prima. */
    private boolean allLow(DigitalPort enables)
    {
        boolean on = true;
        if (enables == null) return true;
        for (DigitalPin g : enables.items()) on &= g.isLow(enabled);
        return on;
    }

    /** Vero se tutti gli enable attivi alti sono alti (nessuno: vero). */
    private boolean allHigh(DigitalPort enables)
    {
        boolean on = true;
        if (enables == null) return true;
        for (DigitalPin g : enables.items()) on &= g.isHigh(enabled);
        return on;
    }
}
