package net.gommagomma.sbam.hardware.memory;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.program.image.MemoryImage;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.physics.Tick;

/**
 * Una memoria parallela di 2^a parole da d bit, con CE# e OE#: quello che RAM e ROM hanno in comune, la lettura.
 *
 * Con CE# e OE# bassi (e chi estende d'accordo) la memoria pilota i dati. Il dato di un indirizzo è valido
 * un tempo d'accesso dopo l'ultimo cambio d'indirizzo, e non prima del tempo d'accesso da OE# da quando la
 * lettura è cominciata; fino ad allora resta il dato precedente. Finita la lettura, i dati si rilasciano dopo
 * il tempo di rilascio.
 */
public abstract class ParallelMemory extends DigitalDevice
{
    private final DigitalPort address;
    private final DigitalPort data;
    private final DigitalPin ce;
    private final DigitalPin oe;
    private final Power power;
    private final Timing timing;                // per il rilascio dei dati
    private final long accessPs, enableAccessPs;
    private final long[] cells;

    private long lastAddress = -1;
    private long addressChangedPs = 0;
    private boolean reading = false;
    private long readStartedPs = 0;

    /**
     * @param addressBits   a: le parole sono 2^a (al massimo 2^24)
     * @param dataBits      d: i bit di una parola
     * @param propagationPs accesso dall'indirizzo: dall'ultimo cambio d'indirizzo al dato valido [ps]
     *                      (sui datasheet: tAA, tACC, tAVQV)
     * @param enablePs      accesso da OE#: da OE# attivo al dato valido sul bus [ps]
     *                      (sui datasheet: tOE, tGLQV, tOEA)
     * @param disablePs     rilascio: da OE# inattivo ai dati in alta impedenza [ps]
     *                      (sui datasheet: tOHZ, tHZ, tGHQZ, tDF, tdis)
     */
    protected ParallelMemory(String name, Family family, int addressBits, int dataBits,
                             long propagationPs, long enablePs, long disablePs)
    {
        super(name);
        if (addressBits < 1 || addressBits > 24) throw new IllegalArgumentException(name + ": da 1 a 24 bit d'indirizzo");
        this.timing = new Timing(propagationPs, propagationPs, enablePs, disablePs);
        this.accessPs = propagationPs;
        this.enableAccessPs = enablePs;
        this.cells = new long[1 << addressBits];
        power = power(20e-12);
        address = port("A", power, family.input(), OutputStage.NONE, addressBits);
        data = port("D", power, family, dataBits);
        ce = input("CE", power, family.input());
        oe = input("OE", power, family.input());
    }

    public final DigitalPort address()  { return address; }
    public final DigitalPort data()     { return data; }
    public final DigitalPin ce()        { return ce; }
    public final DigitalPin oe()        { return oe; }

    /** Quante parole contiene. */
    public final int size()             { return cells.length; }

    /** Il contenuto di una cella. */
    public final long peek(int cell)    { return cells[cell]; }

    /** Scrive una cella da fuori: per caricare il contenuto prima di partire, e per chi estende. */
    protected final void store(int cell, long value)  { cells[cell] = value & data.all(); }

    /** Mette nelle celle un'immagine; gli indirizzi oltre la memoria sono un errore, quelli vuoti restano come sono. */
    protected final void fill(MemoryImage image)
    {
        if (image.end() > cells.length) {
            throw new IllegalArgumentException(name() + String.format(": l'immagine arriva a %X, la memoria ha %X parole", image.end(), cells.length));
        }
        for (int a = 0; a < image.end(); a++) if (image.isWritten(a)) store(a, image.read(a));
    }

    protected final Power supply()      { return power; }

    /** Vero se la lettura è permessa adesso (una RAM non legge mentre scrive). */
    protected abstract boolean readable();

    /** Quello che chi estende fa sulla cella indirizzata, con il chip selezionato, prima della lettura. */
    protected abstract void selected(int cell);

    @Override
    protected final void logic(Tick tick)
    {
        long now = tick.next();
        int cell = (int) address.read();
        if (cell != lastAddress) {
            lastAddress = cell;
            addressChangedPs = now;
        }
        boolean chip = ce.level() == Level.L;
        if (chip) selected(cell);

        boolean read = chip && oe.level() == Level.L && readable();
        if (read && !reading) readStartedPs = now;
        reading = read;

        if (read) data.drive(cells[cell], data.all(), Math.max(addressChangedPs + accessPs, readStartedPs + enableAccessPs));
        else data.drive(0, 0, now, timing);
    }
}
