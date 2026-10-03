package net.gommagomma.sbam.hardware.cpu;

import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.program.InstructionSet;

/**
 * Una CPU von Neumann: programma e dati stanno nella stessa memoria, sul bus. Ogni istruzione si legge dal bus,
 * un ciclo per il codice e uno per ogni byte di operando, poi viene l'eventuale accesso ai dati. Il programma
 * lo tiene una ROM (o una RAM caricata) all'indirizzo 0000, da cui la CPU parte all'accensione.
 *
 * Le sue istruzioni e le loro durate, in cicli macchina:
 *
 *     NOP 1   LDI 2   LDA 4   STA 4   ADD 4   SUB 4   AND 4   OR 4   XOR 4   INC 1
 *     DEC 1   JMP 4   JZ 4   JNZ 4   CALL 6   RET 4   LSP 3   PUSH 2   POP 2   HLT 1
 *
 * (il codice, gli operandi, i trasferimenti di dati; un ciclo in più per ricaricare il programma dopo un salto)
 */
public final class VonNeumannCpu extends Cpu
{
    /** I parametri di tempo sono quelli di Cpu. */
    public VonNeumannCpu(String name, Family family, long propagationPs, long enablePs, long disablePs)
    {
        super(name, family, propagationPs, enablePs, disablePs, null);
        cable(InstructionSet.NOP, 1);
        cable(InstructionSet.LDI, 2);
        cable(InstructionSet.LDA, 4);
        cable(InstructionSet.STA, 4);
        cable(InstructionSet.ADD, 4);
        cable(InstructionSet.SUB, 4);
        cable(InstructionSet.AND, 4);
        cable(InstructionSet.OR, 4);
        cable(InstructionSet.XOR, 4);
        cable(InstructionSet.INC, 1);
        cable(InstructionSet.DEC, 1);
        cable(InstructionSet.JMP, 4);
        cable(InstructionSet.JZ, 4);
        cable(InstructionSet.JNZ, 4);
        cable(InstructionSet.CALL, 6);
        cable(InstructionSet.RET, 4);
        cable(InstructionSet.LSP, 3);
        cable(InstructionSet.PUSH, 2);
        cable(InstructionSet.POP, 2);
        cable(InstructionSet.HLT, 1);
    }
}
