package net.gommagomma.sbam.program;

import java.util.Collections;
import java.util.List;

/** Un'operazione dell'accumulatore con un byte della memoria: A = f(A, [indirizzo]). Legge una volta, all'indirizzo. */
abstract class MemoryOperation extends Instruction
{
    MemoryOperation(String mnemonic, int opcode)
    {
        super(mnemonic, opcode, 2, 1);
    }

    @Override
    public final List<Transfer> transfers(Registers registers, int operand)
    {
        return Collections.singletonList(Transfer.read(operand));
    }

    @Override
    public final void execute(Registers registers, int operand, int[] data)
    {
        registers.accumulator(apply(registers.accumulator(), data[0]));
    }

    /** Il nuovo accumulatore, dall'accumulatore e dal byte letto. */
    abstract int apply(int accumulator, int memory);
}
