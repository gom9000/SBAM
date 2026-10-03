package net.gommagomma.sbam.program;

/** NOP: niente. */
final class Nop extends Instruction
{
    Nop() { super("NOP", 0x00, 0, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) {  }
}
