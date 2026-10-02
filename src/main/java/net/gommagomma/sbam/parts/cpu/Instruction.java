package net.gommagomma.sbam.parts.cpu;

/**
 * Un'istruzione atomica della CPU. Dura un numero di cicli macchina (ognuno di quattro clock, Q1..Q4);
 * in ogni fase di ogni ciclo può agire sui pin (phase), e alla fine produce il suo effetto sui registri
 * e sul flusso del programma (execute).
 *
 * Il suo istante non è scritto nel programma: lo determina la CPU, dalla frequenza del clock e
 * dai cicli delle istruzioni che la precedono.
 */
public abstract class Instruction
{
    private final String mnemonic;
    private final int cycles;

    protected Instruction(String mnemonic, int cycles)
    {
        this.mnemonic = mnemonic;
        this.cycles = cycles;
    }

    /** Quanti cicli macchina dura. */
    public final int cycles() { return cycles; }

    /** Cosa fa sui pin nella fase q (0..3) del ciclo macchina dato. Per default il bus resta a riposo. */
    void phase(int cycle, int q, Cpu cpu)
    {
        if (q == 0) cpu.idle();
    }

    /** L'effetto, alla fine dell'ultimo ciclo: registri e istruzione successiva. */
    abstract void execute(Cpu cpu);

    @Override
    public String toString() { return mnemonic; }
}
