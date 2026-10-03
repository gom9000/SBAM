package net.gommagomma.sbam.program;

/** INC: A = A + 1. */
final class Inc extends Instruction
{
    Inc() { super("INC", 0x05, 0, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { registers.accumulator(registers.accumulator() + 1); }
}
