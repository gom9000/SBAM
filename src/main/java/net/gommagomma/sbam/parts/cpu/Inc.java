package net.gommagomma.sbam.parts.cpu;

/** A = A + 1. */
public final class Inc extends Instruction
{
    public Inc()
    {
        super("INC", 1);
    }

    @Override
    void execute(Cpu cpu) { cpu.accumulator(cpu.accumulator() + 1); }
}
