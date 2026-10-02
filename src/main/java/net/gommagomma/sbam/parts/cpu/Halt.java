package net.gommagomma.sbam.parts.cpu;

/** Si ferma qui: la CPU non esegue più nulla. */
public final class Halt extends Instruction
{
    public Halt()
    {
        super("HALT", 1);
    }

    @Override
    void execute(Cpu cpu) { cpu.halt(); }
}
