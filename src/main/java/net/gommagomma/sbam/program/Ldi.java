package net.gommagomma.sbam.program;

/** LDI valore: A = valore. */
final class Ldi extends Instruction
{
    Ldi() { super("LDI", 0x01, 1, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { registers.accumulator(operand); }
}
