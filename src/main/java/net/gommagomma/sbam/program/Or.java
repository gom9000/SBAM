package net.gommagomma.sbam.program;

/** OR indirizzo: A = A or [indirizzo], bit per bit. */
final class Or extends MemoryOperation
{
    Or() { super("OR", 0x0A); }

    @Override
    int apply(int accumulator, int memory) { return accumulator | memory; }
}
