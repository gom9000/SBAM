package net.gommagomma.sbam.hardware.cpu;

import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.program.InstructionSet;
import net.gommagomma.sbam.program.image.MemoryImage;

/**
 * Una CPU Harvard, come un PIC: il programma sta in una memoria interna, separata dal bus, e codice e operandi
 * si leggono in un ciclo macchina solo; sul bus esterno vanno soltanto i dati. Gli indirizzi di programma che
 * l'immagine non scrive valgono FF (HLT), come una memoria cancellata.
 *
 * Le sue istruzioni e le loro durate, in cicli macchina:
 *
 *     NOP 1   LDI 1   LDA 2   STA 2   ADD 2   SUB 2   AND 2   OR 2   XOR 2   INC 1
 *     DEC 1   JMP 2   JZ 2   JNZ 2   CALL 4   RET 4   LSP 1   PUSH 2   POP 2   HLT 1
 *
 * (un ciclo per leggere l'istruzione, uno per ogni trasferimento di dati, uno per ricaricare il programma dopo un salto)
 */
public final class HarvardCpu extends Cpu
{
    /**
     * I parametri di tempo sono quelli di Cpu.
     * @param program la memoria di programma (da un assemblatore, da un file Intel HEX)
     */
    public HarvardCpu(String name, Family family, long propagationPs, long enablePs, long disablePs, MemoryImage program)
    {
        super(name, family, propagationPs, enablePs, disablePs, program);
        cable(InstructionSet.NOP, 1);
        cable(InstructionSet.LDI, 1);
        cable(InstructionSet.LDA, 2);
        cable(InstructionSet.STA, 2);
        cable(InstructionSet.ADD, 2);
        cable(InstructionSet.SUB, 2);
        cable(InstructionSet.AND, 2);
        cable(InstructionSet.OR, 2);
        cable(InstructionSet.XOR, 2);
        cable(InstructionSet.INC, 1);
        cable(InstructionSet.DEC, 1);
        cable(InstructionSet.JMP, 2);
        cable(InstructionSet.JZ, 2);
        cable(InstructionSet.JNZ, 2);
        cable(InstructionSet.CALL, 4);
        cable(InstructionSet.RET, 4);
        cable(InstructionSet.LSP, 1);
        cable(InstructionSet.PUSH, 2);
        cable(InstructionSet.POP, 2);
        cable(InstructionSet.HLT, 1);
    }
}
