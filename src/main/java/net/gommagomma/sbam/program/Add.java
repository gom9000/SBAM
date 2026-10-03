package net.gommagomma.sbam.program;

/** ADD indirizzo: A = A + [indirizzo]. */
final class Add extends MemoryOperation
{
    Add() { super("ADD", 0x04); }

    @Override
    int apply(int accumulator, int memory) { return accumulator + memory; }
}
