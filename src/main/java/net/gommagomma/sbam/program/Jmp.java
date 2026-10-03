package net.gommagomma.sbam.program;

/** JMP indirizzo: la prossima istruzione è all'indirizzo dato. */
final class Jmp extends Instruction
{
    Jmp() { super("JMP", 0x06, 2, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { registers.jump(operand); }
}
