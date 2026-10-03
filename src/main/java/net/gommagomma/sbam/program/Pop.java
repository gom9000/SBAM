package net.gommagomma.sbam.program;

import java.util.Collections;
import java.util.List;

/** POP: riprende A dallo stack: A = [SP], SP = SP+1. */
final class Pop extends Instruction
{
    Pop() { super("POP", 0x12, 0, 1); }

    @Override
    public List<Transfer> transfers(Registers registers, int operand)
    {
        return Collections.singletonList(Transfer.read(registers.stack()));
    }

    @Override
    public void execute(Registers registers, int operand, int[] data)
    {
        registers.accumulator(data[0]);
        registers.stack(registers.stack() + 1);
    }
}
