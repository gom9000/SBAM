package net.gommagomma.sbam.hardware.memory;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.program.image.MemoryImage;

/**
 * Una RAM statica: una memoria parallela con WE#.
 *
 * Lettura come ogni memoria parallela, con WE# alto. Scrittura: il dato presente sul bus si memorizza sul
 * fronte di salita di WE#, con CE# basso. I bit letti X si memorizzano come 0: il valore reale non sarebbe
 * determinato. Prima di partire si può caricare un'immagine (load), come una RAM tenuta da una batteria.
 * La 62256 è una Memory da 15 e 8.
 */
public final class Memory extends ParallelMemory
{
    private final DigitalPin we;

    /** I parametri di tempo sono quelli di ParallelMemory: accesso dall'indirizzo, accesso da OE#, rilascio. */
    public Memory(String name, Family family, int addressBits, int dataBits,
                  long propagationPs, long enablePs, long disablePs)
    {
        super(name, family, addressBits, dataBits, propagationPs, enablePs, disablePs);
        we = input("WE", supply(), family.input());
    }

    public DigitalPin we()        { return we; }

    /** Scrive una cella da fuori, prima di partire. */
    public void poke(int cell, long value)   { store(cell, value); }

    /** Carica un'immagine prima di partire; gli indirizzi vuoti restano come sono. */
    public Memory load(MemoryImage image)
    {
        fill(image);
        return this;
    }

    @Override
    protected boolean readable()   { return we.level() == Level.H; }

    @Override
    protected void selected(int cell)
    {
        if (we.edge() == Edge.RISING) store(cell, data().read());
    }
}
