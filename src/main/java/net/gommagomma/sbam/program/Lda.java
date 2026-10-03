package net.gommagomma.sbam.program;

/** LDA indirizzo: A = [indirizzo]. */
final class Lda extends MemoryOperation
{
    Lda() { super("LDA", 0x02); }

    @Override
    int apply(int accumulator, int memory) { return memory; }
}
