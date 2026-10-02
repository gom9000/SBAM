package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Come un ingresso carica la linea: correnti d'ingresso, diodi di protezione, capacità.
 *
 * Le correnti seguono la convenzione dei datasheet: positive se entrano nel pin.
 * Un ingresso 74LS basso ha corrente negativa (-0,4 mA): esce dal pin, e chi pilota basso la deve assorbire.
 * Tra le due soglie la corrente passa con continuità da un valore all'altro.
 *
 * I diodi di protezione (se presenti) conducono oltre VDD + clampVolts e sotto GND - clampVolts,
 * con la loro resistenza; i valori sono della famiglia, non del modello.
 * Ognuno è un ramo tra il pin e il pin di alimentazione del device (VDD o GND): la corrente entra
 * nel filo di alimentazione (che, se è spento, viene alimentato così).
 *
 * @param highCurrent corrente d'ingresso a livello alto [A]
 * @param lowCurrent  corrente d'ingresso a livello basso [A]
 * @param clampVolts  tensione di conduzione dei diodi di protezione [V]
 * @param clampOhms   resistenza dei diodi in conduzione [ohm]; infinita se i diodi non ci sono
 * @param capacitance capacità d'ingresso [F]
 */
public final class Loading
{
    private final double highCurrent;
    private final double lowCurrent;
    private final double clampVolts;
    private final double clampOhms;
    private final double capacitance;

    public Loading(double highCurrent, double lowCurrent, double clampVolts, double clampOhms, double capacitance)
    {
        this.highCurrent = highCurrent;
        this.lowCurrent = lowCurrent;
        this.clampVolts = clampVolts;
        this.clampOhms = clampOhms;
        this.capacitance = capacitance;
    }

    /** Un ingresso senza diodi di protezione. */
    public Loading(double highCurrent, double lowCurrent, double capacitance)
    {
        this(highCurrent, lowCurrent, 0.0, Double.POSITIVE_INFINITY, capacitance);
    }

    public double highCurrent() { return highCurrent; }
    public double lowCurrent()  { return lowCurrent; }
    public double clampVolts()  { return clampVolts; }
    public double clampOhms()   { return clampOhms; }
    public double capacitance() { return capacitance; }

    @Override
    public String toString() { return "Loading(" + highCurrent + ", " + lowCurrent + ", " + clampVolts + ", " + clampOhms + ", " + capacitance + ")"; }

    /**
     * @param low  soglia bassa, assoluta [V]
     * @param high soglia alta, assoluta [V]
     */
    void react(Pin pin, Power power, Voltages trial, double low, double high, Reaction out)
    {
        double volts = trial.volts(pin);
        // con VDD uguale a GND (device non alimentato) le soglie relative coincidono: niente transizione graduale
        double span = high - low;
        double f = span > 0.0 ? Math.max(0.0, Math.min(1.0, (volts - low) / span)) : (volts >= high ? 1.0 : 0.0);
        double intoPin = lowCurrent + f * (highCurrent - lowCurrent);
        out.set(pin, 0.0, Double.POSITIVE_INFINITY, -intoPin);

        if (clampOhms == Double.POSITIVE_INFINITY) return;          // nessun diodo
        double vdd = trial.volts(power.vdd());
        double gnd = trial.volts(power.gnd());
        if (volts > vdd + clampVolts) {
            out.set(pin, vdd + clampVolts, clampOhms, 0.0);
            out.set(power.vdd(), volts - clampVolts, clampOhms, 0.0);
        } else if (volts < gnd - clampVolts) {
            out.set(pin, gnd - clampVolts, clampOhms, 0.0);
            out.set(power.gnd(), volts + clampVolts, clampOhms, 0.0);
        }
    }
}
