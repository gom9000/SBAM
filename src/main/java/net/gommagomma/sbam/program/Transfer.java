package net.gommagomma.sbam.program;

/** Un trasferimento sul bus dei dati: una lettura o una scrittura di un byte, a un indirizzo. */
public final class Transfer
{
    private final Access access;
    private final int address;
    private final int value;

    private Transfer(Access access, int address, int value)
    {
        this.access = access;
        this.address = address & 0xFFFF;
        this.value = value & 0xFF;
    }

    /** Legge il byte all'indirizzo. */
    public static Transfer read(int address)              { return new Transfer(Access.READ, address, 0); }

    /** Scrive il byte all'indirizzo. */
    public static Transfer write(int address, int value)  { return new Transfer(Access.WRITE, address, value); }

    public Access access()   { return access; }
    public int address()     { return address; }
    /** Il byte da scrivere (0 per una lettura). */
    public int value()       { return value; }

    @Override
    public String toString() { return access + String.format(" %04X %02X", address, value); }
}
