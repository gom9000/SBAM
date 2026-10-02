package net.gommagomma.sbam.parts.cpu;

/** Niente, per un ciclo. */
public final class Nop extends Instruction
{
    public Nop()
    {
        super("NOP", 1);
    }

    @Override
    void execute(Cpu cpu) { }
}
