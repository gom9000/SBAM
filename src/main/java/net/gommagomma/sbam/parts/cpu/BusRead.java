package net.gommagomma.sbam.parts.cpu;

/**
 * Un'istruzione che legge un byte dal bus, nell'ultimo dei suoi cicli macchina:
 *
 *     Q1: indirizzo sul bus, dati rilasciati      Q2: RD# basso      Q4: campiona i dati, RD# alto
 */
public abstract class BusRead extends Instruction
{
    private final int address;
    private int data;

    BusRead(String mnemonic, int address)
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
        if (q == 0) cpu.addressRead(address);
        if (q == 1) cpu.read(true);
        if (q == 3) {
            data = cpu.sample();
            cpu.read(false);
        }
    }

    @Override
    final void execute(Cpu cpu)
    {
        execute(cpu, data);
    }

    /** L'effetto, con il byte letto. */
    abstract void execute(Cpu cpu, int data);
}
