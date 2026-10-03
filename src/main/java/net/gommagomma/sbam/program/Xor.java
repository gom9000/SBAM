package net.gommagomma.sbam.program;

/** XOR indirizzo: A = A xor [indirizzo], bit per bit. */
final class Xor extends MemoryOperation
{
    Xor() { super("XOR", 0x0B); }

    @Override
    int apply(int accumulator, int memory) { return accumulator ^ memory; }
}
