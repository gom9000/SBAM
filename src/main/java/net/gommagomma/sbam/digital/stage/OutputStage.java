package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Lo stadio d'uscita di un pin digitale: traduce un'intenzione in una caratteristica elettrica.
 *
 * Varianti: TotemPole (H o L), TriState (H, L o Z), OpenDrain (L o Z).
 * Livello alto: HighSide CMOS o TTL. Livello basso: LowSide. Senza uscita: NONE.
 */
public interface OutputStage
{
    /** Vero se lo stadio può eseguire questa intenzione (un open-drain rifiuta H, un totem-pole rifiuta Z). */
    boolean accepts(Drive drive);

    /** L'intenzione con cui lo stadio parte: L per un totem-pole, Z per gli altri. */
    Drive initialDrive();

    /**
     * Dichiara la caratteristica del pin, e dei pin VDD e GND da cui la corrente esce e in cui rientra,
     * per l'intenzione data e le tensioni di prova. Deve essere puro.
     */
    void react(Pin pin, Power power, Drive drive, Voltages trial, Reaction out);

    /**
     * Corrente alla quale il datasheet garantisce ancora i livelli (IOH, IOL) [A]:
     * oltre, il livello non è più garantito. Per le sentinelle.
     */
    double ratedCurrent(Drive drive);

    /** Capacità dell'uscita [F]. */
    double capacitance();

    /** Nessuno stadio d'uscita (un ingresso puro): accetta solo Z e non pilota mai. */
    OutputStage NONE = new OutputStage()
    {
        @Override public boolean accepts(Drive drive) { return drive == Drive.Z; }
        @Override public Drive initialDrive() { return Drive.Z; }
        @Override public void react(Pin pin, Power power, Drive drive, Voltages trial, Reaction out) { }
        @Override public double ratedCurrent(Drive drive) { return 0.0; }
        @Override public double capacitance() { return 0.0; }
        @Override public String toString() { return "nessuna uscita"; }
    };
}
