package net.gommagomma.sbam.digital;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.physics.Port;

import java.util.List;

/**
 * Una porta di pin digitali: la logica la legge e la pilota come una parola (fino a 64 bit, bit 0 = pin 0).
 * Ogni bit resta un DigitalPin con i suoi stadi: la porta non aggiunge fisica, solo la parola.
 */
public class DigitalPort extends Port<DigitalPin>
{
    public DigitalPort(String name, List<DigitalPin> pins)
    {
        super(name, pins);
        if (pins.size() > 64) throw new IllegalArgumentException(name + ": al massimo 64 bit");
    }

    /** Tutti i bit della porta a 1. */
    public long all()
    {
        return width() == 64 ? -1L : (1L << width()) - 1;
    }

    // ------------------------------------------------------------ lettura

    /** La parola letta: 1 dove il pin legge H, 0 dove legge L o X (vedi undefined). */
    public long read()
    {
        return mask(Level.H);
    }

    /** I bit che leggono X: il valore di read() in quei bit non è determinato. */
    public long undefined()
    {
        return mask(Level.X);
    }

    /** I bit il cui livello è cambiato nell'ultimo tick. */
    public long changed()
    {
        long m = 0;
        for (int i = 0; i < width(); i++) {
            if (get(i).edge() != Edge.NONE) m |= 1L << i;
        }
        return m;
    }

    /** I bit la cui intenzione è H. */
    public long driven()
    {
        return intention(Drive.H);
    }

    /** I bit rilasciati (intenzione Z). */
    public long released()
    {
        return intention(Drive.Z);
    }

    // ------------------------------------------------------------ pilotaggio (solo nella logica)

    /** Pilota i bit di enabled al valore di value; gli altri li rilascia (Z). */
    public void drive(long value, long enabled)
    {
        for (int i = 0; i < width(); i++) get(i).drive(bit(value, enabled, i));
    }

    /**
     * Come drive(value, enabled), da un certo istante [ps]: ogni pin riceve la sua intenzione in attesa
     * (vedi DigitalPin.drive(d, t)).
     */
    public void drive(long value, long enabled, long atPs)
    {
        for (int i = 0; i < width(); i++) get(i).drive(bit(value, enabled, i), atPs);
    }

    /** Come drive(value, enabled, t), con il ritardo che i tempi dell'uscita danno per la transizione di ogni bit. */
    public void drive(long value, long enabled, long nowPs, Timing timing)
    {
        for (int i = 0; i < width(); i++) get(i).drive(bit(value, enabled, i), nowPs, timing);
    }

    /**
     * Un buffer: quando enabled ripete la parola letta dalla sorgente, altrimenti si rilascia,
     * con i ritardi che i tempi dell'uscita danno per ogni transizione.
     */
    public void repeat(DigitalPort source, boolean enabled, long nowPs, Timing timing)
    {
        drive(source.read(), enabled ? all() : 0, nowPs, timing);
    }

    /** Pilota tutti i bit. */
    public void drive(long value)
    {
        drive(value, -1L);
    }

    /** Rilascia tutti i bit. */
    public void release()
    {
        drive(0, 0);
    }

    // ------------------------------------------------------------ porzioni

    @Override
    public DigitalPort slice(int from, int to)
    {
        return new DigitalPort(sliceName(from, to), range(from, to));
    }

    /** L'intenzione del bit i di una parola: Z se non abilitato, altrimenti H o L. */
    private static Drive bit(long value, long enabled, int i)
    {
        long b = 1L << i;
        return (enabled & b) == 0 ? Drive.Z : (value & b) != 0 ? Drive.H : Drive.L;
    }

    private long intention(Drive drive)
    {
        long m = 0;
        for (int i = 0; i < width(); i++) {
            if (get(i).driven() == drive) m |= 1L << i;
        }
        return m;
    }

    private long mask(Level level)
    {
        long m = 0;
        for (int i = 0; i < width(); i++) {
            if (get(i).level() == level) m |= 1L << i;
        }
        return m;
    }
}
