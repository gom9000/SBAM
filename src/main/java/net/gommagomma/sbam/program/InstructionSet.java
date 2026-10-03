package net.gommagomma.sbam.program;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Il set di istruzioni, macchina ad accumulatore da 8 bit con indirizzi da 16 e uno stack in memoria:
 *
 *     00 NOP            01 LDI valore     02 LDA indirizzo   03 STA indirizzo   04 ADD indirizzo
 *     05 INC            06 JMP indirizzo  07 JZ indirizzo    08 SUB indirizzo   09 AND indirizzo
 *     0A OR indirizzo   0B XOR indirizzo  0C DEC             0D JNZ indirizzo   0E CALL indirizzo
 *     0F RET            10 LSP indirizzo  11 PUSH            12 POP             FF HLT
 *
 * Gli indirizzi sono di due byte, prima il basso. Lo stack cresce verso il basso: SP è l'indirizzo dell'ultimo
 * byte messo, e all'accensione vale 0000 (il primo byte va a FFFF), finché LSP non lo sposta.
 *
 * È la tabella degli opcode: la usano l'assemblatore per codificare, il disassemblatore per leggere, e le
 * CPU per cablare quello che sanno eseguire, ognuna con le sue durate.
 */
public final class InstructionSet
{
    private InstructionSet() {}

    public static final Instruction NOP = new Nop();
    public static final Instruction LDI = new Ldi();
    public static final Instruction LDA = new Lda();
    public static final Instruction STA = new Sta();
    public static final Instruction ADD = new Add();
    public static final Instruction INC = new Inc();
    public static final Instruction JMP = new Jmp();
    public static final Instruction JZ = new Jz();
    public static final Instruction SUB = new Sub();
    public static final Instruction AND = new And();
    public static final Instruction OR = new Or();
    public static final Instruction XOR = new Xor();
    public static final Instruction DEC = new Dec();
    public static final Instruction JNZ = new Jnz();
    public static final Instruction CALL = new Call();
    public static final Instruction RET = new Ret();
    public static final Instruction LSP = new Lsp();
    public static final Instruction PUSH = new Push();
    public static final Instruction POP = new Pop();
    public static final Instruction HLT = new Halt();

    private static final List<Instruction> ALL = Collections.unmodifiableList(Arrays.asList(
            NOP, LDI, LDA, STA, ADD, INC, JMP, JZ, SUB, AND, OR, XOR, DEC, JNZ, CALL, RET, LSP, PUSH, POP, HLT));

    private static final Instruction[] BY_OPCODE = new Instruction[256];

    static {
        for (Instruction i : ALL) {
            if (BY_OPCODE[i.opcode()] != null) throw new IllegalStateException("codice ripetuto: " + i);
            BY_OPCODE[i.opcode()] = i;
        }
    }

    public static List<Instruction> all()  { return ALL; }

    /** L'istruzione di un codice operativo; null se il codice non è nel set. */
    public static Instruction byOpcode(int opcode)  { return BY_OPCODE[opcode & 0xFF]; }

    /** L'istruzione di un mnemonico (maiuscole o minuscole); null se non c'è. */
    public static Instruction byMnemonic(String mnemonic)
    {
        for (Instruction i : ALL) if (i.mnemonic().equalsIgnoreCase(mnemonic)) return i;
        return null;
    }
}
