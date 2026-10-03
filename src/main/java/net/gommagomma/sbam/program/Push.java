package net.gommagomma.sbam.program;

import java.util.Collections;
import java.util.List;

/** PUSH: mette A nello stack: [SP-1] = A, SP = SP-1. */
final class Push extends Instruction
{
    Push() { super("PUSH", 0x11, 0, 1); }

    @Override
    public List<Transfer> transfers(Registers registers, int operand)
    {
        return Collections.singletonList(Transfer.write(registers.stack() - 1, registers.accumulator()));
    }

    @Override
    public void execute(Registers registers, int operand, int[] data) { registers.stack(registers.stack() - 1); }
}
