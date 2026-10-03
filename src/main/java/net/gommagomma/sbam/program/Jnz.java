package net.gommagomma.sbam.program;

/** JNZ indirizzo: salta all'indirizzo dato se A non è zero. */
final class Jnz extends Instruction
{
    Jnz() { super("JNZ", 0x0D, 2, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { if (!registers.zero()) registers.jump(operand); }
}
