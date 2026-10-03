package net.gommagomma.sbam.hardware.memory;

import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.program.image.MemoryImage;

/**
 * Una memoria a sola lettura (EPROM, EEPROM, flash parallela) già programmata: una memoria parallela senza WE#.
 *
 * Il contenuto arriva con il chip, come dal programmatore: un'immagine (da codice, da un file Intel HEX o
 * binario, da un assemblatore). Gli indirizzi che l'immagine non scrive valgono FF, come in una EPROM
 * cancellata. Un 27C256 è una Rom da 15 e 8.
 */
public final class Rom extends ParallelMemory
{
    /**
     * I parametri di tempo sono quelli di ParallelMemory: accesso dall'indirizzo (tACC), accesso da OE# (tOE),
     * rilascio (tDF).
     * @param content il contenuto programmato
     */
    public Rom(String name, Family family, int addressBits, int dataBits,
               long propagationPs, long enablePs, long disablePs, MemoryImage content)
    {
        super(name, family, addressBits, dataBits, propagationPs, enablePs, disablePs);
        for (int a = 0; a < size(); a++) store(a, -1L);        // cancellata: tutti i bit a 1
        fill(content);
    }

    @Override
    protected boolean readable()   { return true; }

    @Override
    protected void selected(int cell) { }
}
