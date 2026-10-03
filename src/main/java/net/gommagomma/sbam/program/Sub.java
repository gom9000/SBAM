package net.gommagomma.sbam.program;

/** SUB indirizzo: A = A - [indirizzo]. */
final class Sub extends MemoryOperation
{
    Sub() { super("SUB", 0x08); }

    @Override
    int apply(int accumulator, int memory) { return accumulator - memory; }
}
