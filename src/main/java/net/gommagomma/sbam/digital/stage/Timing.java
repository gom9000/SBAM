package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.digital.level.Drive;

/**
 * I ritardi di un'uscita, come li dà il datasheet: dipendono dalla transizione.
 *  - verso H: tPLH; verso L: tPHL;
 *  - da Z (abilitazione): ten; verso Z (rilascio): tdis.
 * È lo strumento con cui un dispositivo applica i suoi ritardi alle uscite: i dispositivi ricevono i tempi
 * come parametri espliciti del costruttore, con i valori del datasheet, e se lo costruiscono da sé.
 *
 * Il valore del datasheet comprende già il fronte dell'uscita misurato sul carico di prova (50 pF per
 * il 74HC), e la rete aggiunge il proprio fronte con il carico reale: la simulazione risulta un po' più
 * lenta del datasheet (pochi ns), cioè dalla parte sicura.
 */
public final class Timing
{
    private final long riseps, fallps, enableps, disableps;

    /** Un'uscita che non si rilascia mai (totem-pole). */
    public Timing(long tplhPs, long tphlPs)
    {
        this(tplhPs, tphlPs, 0, 0);
    }

    public Timing(long tplhPs, long tphlPs, long tenPs, long tdisPs)
    {
        if (tplhPs < 0 || tphlPs < 0 || tenPs < 0 || tdisPs < 0) throw new IllegalArgumentException("ritardi negativi");
        this.riseps = tplhPs;
        this.fallps = tphlPs;
        this.enableps = tenPs;
        this.disableps = tdisPs;
    }

    /** Il ritardo per andare da un'intenzione all'altra [ps]. */
    public long delay(Drive from, Drive to)
    {
        if (to == Drive.Z) return disableps;
        if (from == Drive.Z) return enableps;
        return to == Drive.H ? riseps : fallps;
    }
}
