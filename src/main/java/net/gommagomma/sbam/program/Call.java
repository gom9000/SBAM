package net.gommagomma.sbam.program;

import java.util.Arrays;
import java.util.List;

/**
 * CALL indirizzo: chiama una subroutine. Mette nello stack l'indirizzo a cui tornare (prima il byte alto, a SP-1,
 * poi il basso, a SP-2: in memoria resta in ordine, prima il basso), poi salta.
 */
final class Call extends Instruction
{
    Call() { super("CALL", 0x0E, 2, 2); }

    @Override
    public List<Transfer> transfers(Registers registers, int operand)
    {
        int back = registers.following(), sp = registers.stack();
        return Arrays.asList(Transfer.write(sp - 1, back >> 8), Transfer.write(sp - 2, back));
    }

    @Override
    public void execute(Registers registers, int operand, int[] data)
    {
        registers.stack(registers.stack() - 2);
        registers.jump(operand);
    }
}
