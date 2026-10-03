package net.gommagomma.sbam.program;

/** LSP indirizzo: il puntatore allo stack vale l'indirizzo dato (il primo byte messo nello stack andrà all'indirizzo prima). */
final class Lsp extends Instruction
{
    Lsp() { super("LSP", 0x10, 2, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { registers.stack(operand); }
}
