package net.gommagomma.sbam.parts.cpu;

/** A = valore. */
public final class Ldi extends Instruction
{
    private final int value;

    public Ldi(int value)
    {
        super(String.format("LDI %02X", value & 0xFF), 1);
        this.value = value;
    }

    @Override
    void execute(Cpu cpu) { cpu.accumulator(value); }
}
