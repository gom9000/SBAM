package net.gommagomma.sbam.parts.cpu;

/** Salta all'istruzione di indice dato se A è zero. */
public final class Jz extends Instruction
{
    private final int target;

    public Jz(int target)
    {
        super("JZ " + target, 2);
        this.target = target;
    }

    @Override
    void execute(Cpu cpu) { if (cpu.zero()) cpu.jump(target); }
}
