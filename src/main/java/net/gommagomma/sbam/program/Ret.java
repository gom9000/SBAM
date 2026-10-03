package net.gommagomma.sbam.program;

import java.util.Arrays;
import java.util.List;

/** RET: torna da una subroutine all'indirizzo che CALL ha messo nello stack, e lo toglie dallo stack. */
final class Ret extends Instruction
{
    Ret() { super("RET", 0x0F, 0, 2); }

    @Override
    public List<Transfer> transfers(Registers registers, int operand)
    {
        int sp = registers.stack();
        return Arrays.asList(Transfer.read(sp), Transfer.read(sp + 1));
    }

    @Override
    public void execute(Registers registers, int operand, int[] data)
    {
        registers.jump(data[0] | (data[1] << 8));
        registers.stack(registers.stack() + 2);
    }
}
