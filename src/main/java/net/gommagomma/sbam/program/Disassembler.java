package net.gommagomma.sbam.program;

import net.gommagomma.sbam.program.image.MemoryImage;

/**
 * Il disassemblatore: da un'istruzione e il suo operando al testo, nella stessa forma che legge l'assemblatore
 * (valori e indirizzi in esadecimale con 0x). Serve a chi guarda un programma che gira.
 */
public final class Disassembler
{
    private Disassembler() {}

    /** "LDA 0x8000", "LDI 0x5A", "INC". */
    public static String text(Instruction instruction, int operand)
    {
        switch (instruction.operandBytes()) {
            case 0: return instruction.mnemonic();
            case 1: return String.format("%s 0x%02X", instruction.mnemonic(), operand & 0xFF);
            case 2: return String.format("%s 0x%04X", instruction.mnemonic(), operand & 0xFFFF);
            default: throw new IllegalStateException(instruction + ": " + instruction.operandBytes() + " byte di operando");
        }
    }

    /** L'istruzione all'indirizzo dato di un'immagine, in testo; "DB 0x.." se il codice non è nel set. */
    public static String text(MemoryImage image, int address)
    {
        int code = image.read(address, 0xFF);
        Instruction i = InstructionSet.byOpcode(code);
        if (i == null) return String.format("DB 0x%02X", code);
        int operand = 0;
        for (int b = 0; b < i.operandBytes(); b++) operand |= image.read((address + 1 + b) & 0xFFFF, 0xFF) << (8 * b);
        return text(i, operand);
    }
}
