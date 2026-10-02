package net.gommagomma.sbam.parts.cpu;

/** [indirizzo] = A. */
public final class Sta extends BusWrite
{
    public Sta(int address)
    {
        super("STA", address);
    }

    @Override
    int data(Cpu cpu) { return cpu.accumulator(); }

    @Override
    void execute(Cpu cpu) { }
}
