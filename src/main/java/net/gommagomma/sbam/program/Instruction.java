package net.gommagomma.sbam.program;

import java.util.Collections;
import java.util.List;

/**
 * Un'istruzione del set, come la descrive la tabella degli opcode di un datasheet: il mnemonico, il codice
 * operativo, quanti byte di operando lo seguono in memoria, quali trasferimenti fa sul bus dei dati, e
 * l'effetto sui registri. È una sola per codice (non porta con sé l'operando: l'operando è nei byte che
 * seguono il codice, nel programma).
 *
 * Non dice quanto dura: la durata è di ogni CPU che la esegue, nella sua tabella (vedi hardware.cpu).
 */
public abstract class Instruction
{
    private final String mnemonic;
    private final int opcode;
    private final int operandBytes;
    private final int dataCycles;

    /**
     * @param operandBytes 0, 1 (un valore) o 2 (un indirizzo, prima il byte basso)
     * @param dataCycles   quanti trasferimenti sul bus dei dati fa, sempre gli stessi (vedi transfers)
     */
    protected Instruction(String mnemonic, int opcode, int operandBytes, int dataCycles)
    {
        if (operandBytes < 0 || operandBytes > 2) throw new IllegalArgumentException(mnemonic + ": da 0 a 2 byte di operando");
        if (dataCycles < 0) throw new IllegalArgumentException(mnemonic + ": trasferimenti negativi");
        this.mnemonic = mnemonic;
        this.opcode = opcode;
        this.operandBytes = operandBytes;
        this.dataCycles = dataCycles;
    }

    public final String mnemonic()    { return mnemonic; }
    public final int opcode()         { return opcode; }
    public final int operandBytes()   { return operandBytes; }
    public final int dataCycles()     { return dataCycles; }

    /**
     * I trasferimenti sul bus dei dati, in ordine: tanti quanti dataCycles. Si calcolano dopo aver letto
     * l'operando, con i registri di prima dell'effetto. Di norma nessuno.
     */
    public List<Transfer> transfers(Registers registers, int operand)
    {
        return Collections.emptyList();
    }

    /**
     * L'effetto, alla fine dell'ultimo ciclo.
     * @param operand l'operando (0 se non c'è)
     * @param data    i byte letti nei trasferimenti, nel loro ordine (0 per le scritture)
     */
    public abstract void execute(Registers registers, int operand, int[] data);

    @Override
    public String toString() { return mnemonic; }
}
