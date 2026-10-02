package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Lo stadio d'ingresso di un pin digitale: come legge la tensione e come carica la linea.
 *
 * Varianti: BufferInput (tra le soglie legge X) e SchmittInput (tra le soglie conserva
 * il livello letto prima). Senza ingresso: NONE.
 */
public interface InputStage
{
    /**
     * Il livello letto.
     *
     * @param volts    tensione del nodo del pin, misurata da GND [V]
     * @param vdd      tensione di alimentazione, VDD meno GND [V]
     * @param previous livello letto al tick precedente (serve all'isteresi)
     */
    Level read(double volts, double vdd, Level previous);

    /**
     * Il comportamento elettrico dell'ingresso durante l'assestamento: correnti d'ingresso,
     * diodi di protezione (che scambiano corrente con i pin VDD e GND). Deve essere puro.
     */
    void react(Pin pin, Power power, Voltages trial, Reaction out);

    /** Capacità dell'ingresso [F]. */
    double capacitance();

    /** Nessuno stadio d'ingresso (un'uscita pura): legge sempre X e non carica la linea. */
    InputStage NONE = new InputStage()
    {
        @Override public Level read(double volts, double vdd, Level previous) { return Level.X; }
        @Override public void react(Pin pin, Power power, Voltages trial, Reaction out) { }
        @Override public double capacitance() { return 0.0; }
        @Override public String toString() { return "nessun ingresso"; }
    };
}
