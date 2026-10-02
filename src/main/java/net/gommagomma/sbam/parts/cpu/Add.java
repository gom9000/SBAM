package net.gommagomma.sbam.parts.cpu;

/** A = A + [indirizzo]. */
public final class Add extends BusRead
{
    public Add(int address)
    {
        super("ADD", address);
    }

    @Override
    void execute(Cpu cpu, int data) { cpu.accumulator(cpu.accumulator() + data); }
}
