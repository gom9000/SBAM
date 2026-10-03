package net.gommagomma.sbam.program;

/** AND indirizzo: A = A and [indirizzo], bit per bit. */
final class And extends MemoryOperation
{
    And() { super("AND", 0x09); }

    @Override
    int apply(int accumulator, int memory) { return accumulator & memory; }
}
