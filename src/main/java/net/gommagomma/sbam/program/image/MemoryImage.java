package net.gommagomma.sbam.program.image;

import java.util.Arrays;

/**
 * Un'immagine di memoria: quali byte stanno a quali indirizzi, prima di metterli in un chip.
 * È quello che un programmatore di EPROM carica da un file e brucia, e quello che produce un assemblatore.
 *
 * Gli indirizzi non scritti restano vuoti: chi riceve l'immagine decide che cosa contengono
 * (una EPROM cancellata ha FF, una RAM appena accesa quello che capita).
 */
public final class MemoryImage
{
    private int[] bytes = new int[0];
    private boolean[] written = new boolean[0];
    private int end = 0;

    /** Scrive dei byte da un indirizzo in poi; riscrivere un indirizzo è un errore (due cose nello stesso posto). */
    public MemoryImage write(int address, int... values)
    {
        if (address < 0) throw new IllegalArgumentException("indirizzo negativo: " + address);
        ensure(address + values.length);
        for (int i = 0; i < values.length; i++) {
            int a = address + i;
            if (written[a]) throw new IllegalArgumentException(String.format("indirizzo %04X scritto due volte", a));
            if (values[i] < 0 || values[i] > 0xFF) throw new IllegalArgumentException(String.format("a %04X: %d non sta in un byte", a, values[i]));
            bytes[a] = values[i];
            written[a] = true;
        }
        end = Math.max(end, address + values.length);
        return this;
    }

    /** Il primo indirizzo dopo l'ultimo scritto. */
    public int end()                       { return end; }

    public boolean isWritten(int address)  { return address >= 0 && address < end && written[address]; }

    /** Il byte a un indirizzo scritto. */
    public int read(int address)
    {
        if (!isWritten(address)) throw new IllegalArgumentException(String.format("indirizzo %04X non scritto", address));
        return bytes[address];
    }

    /** Il byte a un indirizzo, o il valore dato se l'indirizzo è vuoto. */
    public int read(int address, int empty)
    {
        return isWritten(address) ? bytes[address] : empty;
    }

    private void ensure(int size)
    {
        if (size <= bytes.length) return;
        int n = Math.max(size, Math.max(256, bytes.length * 2));
        bytes = Arrays.copyOf(bytes, n);
        written = Arrays.copyOf(written, n);
    }
}
