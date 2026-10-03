package net.gommagomma.sbam.program;

/** JZ indirizzo: salta all'indirizzo dato se A è zero. */
final class Jz extends Instruction
{
    Jz() { super("JZ", 0x07, 2, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { if (registers.zero()) registers.jump(operand); }
}
