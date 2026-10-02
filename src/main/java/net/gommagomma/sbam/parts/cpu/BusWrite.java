package net.gommagomma.sbam.parts.cpu;

/**
 * Un'istruzione che scrive un byte sul bus, nell'ultimo dei suoi cicli macchina:
 *
 *     Q1: indirizzo e dato sul bus      Q2: WR# basso      Q4: WR# alto (il dato resta fino al ciclo dopo)
 */
public abstract class BusWrite extends Instruction
{
    private final int address;

    BusWrite(String mnemonic, int address)
    {
        super(mnemonic + String.format(" %04X", address), 2);
        this.address = address;
    }

    @Override
    final void phase(int cycle, int q, Cpu cpu)
    {
        if (cycle < cycles() - 1) {
            super.phase(cycle, q, cpu);
            return;
        }
        if (q == 0) cpu.addressWrite(address, data(cpu));
        if (q == 1) cpu.write(true);
        if (q == 3) cpu.write(false);
    }

    /** Il byte da scrivere. */
    abstract int data(Cpu cpu);
}
