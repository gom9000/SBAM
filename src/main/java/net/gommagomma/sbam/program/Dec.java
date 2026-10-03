package net.gommagomma.sbam.program;

/** DEC: A = A - 1. */
final class Dec extends Instruction
{
    Dec() { super("DEC", 0x0C, 0, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { registers.accumulator(registers.accumulator() - 1); }
}
