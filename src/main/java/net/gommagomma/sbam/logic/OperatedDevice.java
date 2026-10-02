package net.gommagomma.sbam.logic;

import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

/**
 * Un device azionato dall'esterno della rete: chi opera al banco sposta i suoi organi meccanici
 * (levette, pulsanti). Lo stimolo arriva tutto in partenza, perché la simulazione procede da sola:
 * un array di stimoli, ognuno con il suo istante e il suo dato, in ordine di tempo.
 *
 * È lo stesso schema delle intenzioni di un pin (un presente e ciò che attende il suo istante), ma qui
 * la coda è multipla e data in partenza: ogni stimolo diventa presente a suo tempo, e il più vecchio
 * tra quelli arrivati è la posizione presente.
 *
 * È l'unico modo in cui l'esterno entra nella rete, e lo fa come stato meccanico del device
 * (position), mai come tensioni. Uno stimolo a 0 ps è la posizione all'accensione; gli altri
 * si applicano nell'update del tick in cui scadono, e valgono dal tick successivo.
 * Chi estende legge position() nella sua react.
 */
public abstract class OperatedDevice extends Device
{
    private final Stimulus[] stimuli;
    private int next = 0;
    private long position = 0;

    protected OperatedDevice(String name, Stimulus[] stimuli)
    {
        super(name);
        for (int i = 1; i < stimuli.length; i++) {
            if (stimuli[i].timePs() <= stimuli[i - 1].timePs()) {
                throw new IllegalArgumentException(name + ": stimoli non in ordine di tempo: " + stimuli[i - 1] + ", " + stimuli[i]);
            }
        }
        this.stimuli = stimuli.clone();
        while (next < this.stimuli.length && this.stimuli[next].timePs() == 0) position = this.stimuli[next++].value();
    }

    /** La posizione attuale: l'ultimo dato ricevuto (0 se nessuno). */
    public final long position() { return position; }

    @Override
    protected final void update(Tick tick, Voltages settled)
    {
        while (next < stimuli.length && stimuli[next].timePs() <= tick.next()) position = stimuli[next++].value();
    }
}
