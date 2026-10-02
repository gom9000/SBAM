package net.gommagomma.sbam.parts.timing;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.physics.Tick;

/**
 * Un oscillatore quarzato in contenitore (il "can" a 4 pin): VDD, GND e un'uscita di clock.
 * Dopo l'accensione l'uscita resta bassa per il tempo di avvio, poi oscilla alla sua frequenza
 * con il duty cycle dato. Lo stadio d'uscita è quello della famiglia.
 *
 * Il periodo si misura in tick: conviene che metà periodo sia un multiplo del tick.
 */
public final class Oscillator extends DigitalDevice
{
    private final Power power;
    private final DigitalPin out;
    private final long periodPs;
    private final long highPs;
    private final long startPs;

    /**
     * @param hertz   frequenza [Hz]
     * @param duty    frazione del periodo a livello alto (0,5 per un'onda quadra)
     * @param startPs avvio: dall'accensione alla prima oscillazione, uscita bassa fino ad allora [ps]
     *                (sui datasheet: start-up time, tSU, tOSC)
     */
    public Oscillator(String name, Family family, double hertz, double duty, long startPs)
    {
        super(name);
        if (!(hertz > 0) || !(duty > 0 && duty < 1)) throw new IllegalArgumentException(name + ": frequenza o duty non validi");
        this.periodPs = Math.round(1e12 / hertz);
        this.highPs = Math.round(periodPs * duty);
        this.startPs = startPs;
        power = power("VDD", "GND", 5e-12);
        out = output("OUT", power, family.output());
        out.drive(Drive.L);
    }

    public DigitalPin out()   { return out; }
    public long periodPs()    { return periodPs; }

    @Override
    protected void logic(Tick tick)
    {
        long t = tick.next() - startPs;
        out.drive(t >= 0 && t % periodPs < highPs ? Drive.H : Drive.L);
    }
}
