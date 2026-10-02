package net.gommagomma.sbam.parts.cpu;

/** Salta all'istruzione di indice dato. */
public final class Jmp extends Instruction
{
    private final int target;

    public Jmp(int target)
    {
        super("JMP " + target, 2);
        this.target = target;
    }

    @Override
    void execute(Cpu cpu) { cpu.jump(target); }
}
