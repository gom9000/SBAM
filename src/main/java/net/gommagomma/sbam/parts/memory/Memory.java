package net.gommagomma.sbam.parts.memory;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.physics.Tick;

/**
 * Una RAM statica di 2^a parole da d bit, con CE#, OE# e WE#.
 *
 * Lettura: con CE# e OE# bassi e WE# alto la RAM pilota i dati. Il dato di un indirizzo è valido
 * un tempo d'accesso dopo l'ultimo cambio d'indirizzo, e non prima del tempo d'accesso da OE# da quando
 * la lettura è cominciata; fino ad allora resta il dato precedente. Finita la lettura, i dati si
 * rilasciano dopo il tempo di rilascio.
 *
 * Scrittura: il dato presente sul bus si memorizza sul fronte di salita di WE#, con CE# basso.
 * I bit letti X si memorizzano come 0: il valore reale non sarebbe determinato.
 * La 62256 è una Memory da 15 e 8.
 */
public final class Memory extends DigitalDevice
{
    private final DigitalPort address;
    private final DigitalPort data;
    private final DigitalPin ce;
    private final DigitalPin oe;
    private final DigitalPin we;
    private final Timing timing;           // per il rilascio dei dati
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
     *                      (sui datasheet: tOHZ, tHZ, tGHQZ, tdis)
     */
    public Memory(String name, Family family, int addressBits, int dataBits,
                  long propagationPs, long enablePs, long disablePs)
    {
        super(name);
        if (addressBits < 1 || addressBits > 24) throw new IllegalArgumentException(name + ": da 1 a 24 bit d'indirizzo");
        this.timing = new Timing(propagationPs, propagationPs, enablePs, disablePs);
        this.accessPs = propagationPs;
        this.enableAccessPs = enablePs;
        this.cells = new long[1 << addressBits];
        Power p = power("VCC", "GND", 20e-12);
        address = port("A", p, family.input(), OutputStage.NONE, addressBits);
        data = port("D", p, family, dataBits);
        ce = input("CE", p, family.input());
        oe = input("OE", p, family.input());
        we = input("WE", p, family.input());
    }

    public DigitalPort address()  { return address; }
    public DigitalPort data()     { return data; }
    public DigitalPin ce()        { return ce; }
    public DigitalPin oe()        { return oe; }
    public DigitalPin we()        { return we; }

    /** Il contenuto di una cella, per i test e per caricare la memoria prima di partire. */
    public long peek(int cell)               { return cells[cell]; }
    public void poke(int cell, long value)   { cells[cell] = value & data.all(); }

    @Override
    protected void logic(Tick tick)
    {
        long now = tick.next();
        int cell = (int) address.read();
        if (cell != lastAddress) {
            lastAddress = cell;
            addressChangedPs = now;
        }

        boolean selected = ce.level() == Level.L;
        if (selected && we.edge() == Edge.RISING) cells[cell] = data.read();

        boolean read = selected && oe.level() == Level.L && we.level() == Level.H;
        if (read && !reading) readStartedPs = now;
        reading = read;

        if (read) {
            data.drive(cells[cell], data.all(), Math.max(addressChangedPs + accessPs, readStartedPs + enableAccessPs));
        } else {
            data.drive(0, 0, now, timing);
        }
    }
}
