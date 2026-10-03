package net.gommagomma.sbam.program;

import net.gommagomma.sbam.program.image.MemoryImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblerTest
{
    @Test
    void codesOperandsLabelsAndDirectives()
    {
        Assembler.Result p = Assembler.assemble(String.join("\n",
                "; prova",
                "LED     EQU 0xC000",
                "        ORG $0100",
                "start:  LDI 10          ; decimale",
                "        STA LED",
                "loop:   JZ fine         ; in avanti",
                "        JMP loop",
                "fine:   HLT",
                "tab:    DB 1, 0b101, 0FFh, tab-0x100"));
        MemoryImage m = p.image();
        int[] expected = { 0x01, 10, 0x03, 0x00, 0xC0, 0x07, 0x0B, 0x01, 0x06, 0x05, 0x01, 0xFF, 1, 5, 0xFF, 0x0C };
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], m.read(0x100 + i), "byte " + i);
        assertFalse(m.isWritten(0x0FF));
        assertEquals(0x100 + expected.length, m.end());
        assertEquals(0x0105, p.symbol("loop"));
        assertEquals(0xC000, p.symbol("led"));
        assertTrue(p.listing().get(3).startsWith("0100  01 0A"), p.listing().get(3));
    }

    @Test
    void mistakesSayTheLine()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Assembler.assemble("NOP\nLDA nessuno"));
        assertEquals("programma:2: nome non definito: nessuno", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Assembler.assemble("FOO 1"));          // istruzione sconosciuta
        assertThrows(IllegalArgumentException.class, () -> Assembler.assemble("LDI 300"));        // non sta in un byte
        assertThrows(IllegalArgumentException.class, () -> Assembler.assemble("INC 1"));          // operando di troppo
        assertThrows(IllegalArgumentException.class, () -> Assembler.assemble("a: NOP\na: NOP")); // nome ripetuto
        assertThrows(IllegalArgumentException.class, () -> Assembler.assemble("NOP\nORG 0\nNOP")); // due cose nello stesso posto
    }

    @Test
    void theDisassemblerWritesWhatTheAssemblerReads()
    {
        String source = "LDI 0x5A\nSTA 0x8000\nCALL 0x0123\nRET\nDB 0x42";
        MemoryImage m = Assembler.assemble(source).image();
        assertEquals("LDI 0x5A", Disassembler.text(m, 0));
        assertEquals("STA 0x8000", Disassembler.text(m, 2));
        assertEquals("CALL 0x0123", Disassembler.text(m, 5));
        assertEquals("RET", Disassembler.text(m, 8));
        assertEquals("DB 0x42", Disassembler.text(m, 9));
        assertEquals(Assembler.assemble("STA 0x8000").image().read(1), Assembler.assemble(Disassembler.text(m, 2)).image().read(1));
    }
}
