package net.gommagomma.sbam.program;

import java.util.Collections;
import java.util.List;

/** STA indirizzo: [indirizzo] = A. */
final class Sta extends Instruction
{
    Sta() { super("STA", 0x03, 2, 1); }

    @Override
    public List<Transfer> transfers(Registers registers, int operand)
    {
        return Collections.singletonList(Transfer.write(operand, registers.accumulator()));
    }

    @Override
    public void execute(Registers registers, int operand, int[] data) { }
}
