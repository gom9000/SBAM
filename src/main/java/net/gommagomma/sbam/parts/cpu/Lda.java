package net.gommagomma.sbam.parts.cpu;

/** A = [indirizzo]. */
public final class Lda extends BusRead
{
    public Lda(int address)
    {
        super("LDA", address);
    }

    @Override
    void execute(Cpu cpu, int data) { cpu.accumulator(data); }
}
