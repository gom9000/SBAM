package net.gommagomma.sbam.hardware.logic;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.logic.LogicFunction;
import net.gommagomma.sbam.physics.Tick;

/**
 * Una porta logica singola: N ingressi, un'uscita, la sua alimentazione.
 * Non è un chip (niente contenitore da quattro porte): è la porta, con la funzione, la famiglia
 * (soglie, stadio d'uscita) e i ritardi del datasheet. Un 74HC14 è una porta NOT della famiglia
 * con ingresso Schmitt; un 74HC03 è una NAND di una famiglia con uscita open-drain.
 *
 * Se la funzione dà X (un ingresso indefinito che decide il risultato), l'uscita mantiene
 * l'intenzione che aveva: non c'è nulla di nuovo da decidere.
 */
public final class Gate extends DigitalDevice
{
    private final Power power;
    private final DigitalPort inputs;
    private final DigitalPin y;
    private final LogicFunction function;
    private final Timing timing;
    private final Level[] levels;

    /**
     * @param inputs        numero di ingressi
     * @param propagationPs ritardo di propagazione: da un ingresso che cambia all'uscita che cambia [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     */
    public Gate(String name, Family family, LogicFunction function, int inputs, long propagationPs)
    {
        super(name);
        this.function = function;
        this.timing = new Timing(propagationPs, propagationPs);
        power = power(5e-12);
        this.inputs = port("A", power, family.input(), OutputStage.NONE, inputs);
        y = output("Y", power, family.output());
        levels = new Level[inputs];
    }

    public DigitalPort inputs()    { return inputs; }
    public DigitalPin in(int i)    { return inputs.get(i); }
    public DigitalPin y()          { return y; }
    public LogicFunction function(){ return function; }

    @Override
    protected void logic(Tick tick)
    {
        for (int i = 0; i < levels.length; i++) levels[i] = inputs.get(i).level();
        Level result = function.apply(levels);
        if (result == Level.H) y.drive(Drive.H, tick.next(), timing);
        if (result == Level.L) y.drive(Drive.L, tick.next(), timing);
    }
}
