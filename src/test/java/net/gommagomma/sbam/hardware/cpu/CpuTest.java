package net.gommagomma.sbam.hardware.cpu;

import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.digital.ContentionSentinel;
import net.gommagomma.sbam.logic.LogicFunction;
import net.gommagomma.sbam.hardware.logic.Gate;
import net.gommagomma.sbam.hardware.memory.Memory;
import net.gommagomma.sbam.program.image.MemoryImage;
import net.gommagomma.sbam.hardware.memory.Rom;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.hardware.timing.Oscillator;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import net.gommagomma.sbam.program.Assembler;
import net.gommagomma.sbam.program.InstructionSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le due CPU, con lo stesso set di istruzioni: Harvard (programma interno) e von Neumann (programma in ROM). */
class CpuTest
{
    /** Un banco: alimentazione, clock a 4 MHz (1 us per ciclo macchina), la CPU e i suoi bus. */
    private static final class Bench
    {
        final Engine engine = new Engine(1_000);
        final Wire rail = new Wire("+5V", 100e-12);
        final Wire ground;
        final Bus a = Bus.of("A", 16, 10e-12), d = Bus.of("D", 8, 10e-12);
        final Wire rd = new Wire("RD", 10e-12), wr = new Wire("WR", 10e-12);
        final EventLog log = new EventLog();
        final Cpu cpu;

        Bench(Cpu cpu)
        {
            Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
            vcc.out().connect(rail);
            ground = Ground.of(engine);
            Oscillator x1 = engine.add(new Oscillator("X1", Families.HC_GATE, 4e6, 0.5, 0));
            this.cpu = engine.add(cpu);
            x1.vdd().connect(rail);  x1.gnd().connect(ground);
            cpu.vdd().connect(rail); cpu.gnd().connect(ground);
            Wire clk = new Wire("CLK", 10e-12);
            x1.out().connect(clk);
            cpu.clk().connect(clk);
            cpu.a().connect(a);
            cpu.d().connect(d);
            cpu.rd().connect(rd);
            cpu.wr().connect(wr);
            engine.add(new ContentionSentinel(log));
        }

        /** Una RAM 62256 da 32K: con CE# su A15 sta a 0000-7FFF; con CE# su A15 negato, a 8000-FFFF. */
        Memory ram(boolean high)
        {
            Memory ram = engine.add(new Memory("RAM", Families.HCT, 15, 8, 70_000, 35_000, 25_000));
            ram.vdd().connect(rail); ram.gnd().connect(ground);
            ram.address().connect(a.slice(0, 15));
            ram.data().connect(d);
            ram.oe().connect(rd);
            ram.we().connect(wr);
            if (!high) {
                ram.ce().connect(a.get(15));
            } else {
                Gate not = engine.add(new Gate("U1", Families.HC_GATE, LogicFunction.NOT, 1, 8_000));   // 74HC04
                not.vdd().connect(rail); not.gnd().connect(ground);
                not.in(0).connect(a.get(15));
                Wire ce = new Wire("RAMCE", 5e-12);
                not.y().connect(ce);
                ram.ce().connect(ce);
            }
            return ram;
        }

        /** Una ROM 27C256 a 0000-7FFF, con CE# su A15. */
        Rom rom(MemoryImage content)
        {
            Rom rom = engine.add(new Rom("ROM", Families.HC, 15, 8, 150_000, 75_000, 50_000, content));
            rom.vdd().connect(rail); rom.gnd().connect(ground);
            rom.address().connect(a.slice(0, 15));
            rom.data().connect(d);
            rom.oe().connect(rd);
            rom.ce().connect(a.get(15));
            return rom;
        }
    }

    private static final String STORE_AND_READ_BACK = String.join("\n",
            "        LDI 0x5A",
            "        STA DATA",
            "        LDI 0",
            "        LDA DATA",
            "        INC",
            "        STA DATA+1",
            "        HLT");

    @Test
    void harvardRunsItsInternalProgramAgainstARam()
    {
        MemoryImage program = Assembler.assemble("DATA EQU 0x0010\n" + STORE_AND_READ_BACK).image();
        Bench b = new Bench(new HarvardCpu("CPU", Families.HC, 10_000, 10_000, 10_000, program));
        Memory ram = b.ram(false);

        // 1 + 2 + 1 + 2 + 1 + 2 + 1 = 10 cicli macchina, 10 us
        b.engine.runUntil(15_000_000);
        assertTrue(b.cpu.halted(), "arrivata a HLT");
        assertNull(b.cpu.fault());
        assertEquals(0x5AL, ram.peek(0x0010), "STA scrive in RAM");
        assertEquals(0x5BL, ram.peek(0x0011), "LDA rilegge, INC, STA");
        assertEquals(0x5B, b.cpu.accumulator());
        assertTrue(b.log.events().isEmpty(), "nessuno scontro sul bus: " + b.log.events());
    }

    @Test
    void vonNeumannReadsItsProgramFromARomOnTheSameBus()
    {
        MemoryImage program = Assembler.assemble("DATA EQU 0x8010\n" + STORE_AND_READ_BACK).image();
        Bench b = new Bench(new VonNeumannCpu("CPU", Families.HC, 10_000, 10_000, 10_000));
        b.rom(program);
        Memory ram = b.ram(true);

        // LDI 2 + STA 4 + LDI 2 + LDA 4 + INC 1 + STA 4 + HLT 1 = 18 cicli macchina, 18 us
        b.engine.runUntil(17_500_000);
        assertTrue(!b.cpu.halted(), "non ancora: legge anche il codice dal bus");
        b.engine.runUntil(19_000_000);
        assertTrue(b.cpu.halted(), "arrivata a HLT");
        assertNull(b.cpu.fault());
        assertEquals(0x5AL, ram.peek(0x0010));
        assertEquals(0x5BL, ram.peek(0x0011));
        assertTrue(b.log.events().isEmpty(), "ROM e RAM non si scontrano sul bus: " + b.log.events());
    }

    @Test
    void theTimeOfEachInstructionComesFromTheClockAndItsCycles()
    {
        MemoryImage program = Assembler.assemble("NOP\nJMP end\nNOP\nend: HLT").image();   // NOP 0, JMP 1, NOP 4, HLT 5

        Bench h = new Bench(new HarvardCpu("CPU", Families.HC, 10_000, 10_000, 10_000, program));
        h.engine.runUntil(1_100_000);
        assertEquals(1, h.cpu.pc(), "NOP: un ciclo macchina (4 clock, 1 us)");
        h.engine.runUntil(2_100_000);
        assertEquals(1, h.cpu.pc(), "JMP dura due cicli: ancora in corso");
        h.engine.runUntil(3_100_000);
        assertEquals(5, h.cpu.pc(), "saltato a HLT, senza passare dalla NOP a 4");

        Bench v = new Bench(new VonNeumannCpu("CPU", Families.HC, 10_000, 10_000, 10_000));
        v.rom(program);
        v.engine.runUntil(1_100_000);
        assertEquals(1, v.cpu.pc(), "NOP: il ciclo che la legge");
        v.engine.runUntil(4_600_000);
        assertEquals(1, v.cpu.pc(), "JMP: codice, due byte d'indirizzo, un ciclo interno");
        v.engine.runUntil(5_100_000);
        assertEquals(5, v.cpu.pc());
    }

    @Test
    void anUnknownOpcodeStopsTheCpu()
    {
        MemoryImage program = Assembler.assemble("NOP\nDB 0x42").image();
        Bench h = new Bench(new HarvardCpu("CPU", Families.HC, 10_000, 10_000, 10_000, program));
        h.engine.runUntil(3_000_000);
        assertTrue(h.cpu.halted());
        assertEquals("codice sconosciuto 42 a 0001", h.cpu.fault());
    }

    /** Un programma che usa tutto il set: subroutine, stack, aritmetica e logica, salti condizionati. */
    private static String everything(int data, int top)
    {
        return String.join("\n",
                String.format("N      EQU 0x%04X", data),
                "SUM    EQU N+1",
                "FIVE   EQU N+2",
                "MASK   EQU N+3",
                "MASK2  EQU N+4",
                "ONE    EQU N+5",
                "RESULT EQU N+6",
                String.format("       LSP 0x%04X", top),
                "       LDI 5",
                "       STA FIVE",
                "       LDI 0xFF",
                "       STA MASK",
                "       LDI 0x3C",
                "       STA MASK2",
                "       LDI 1",
                "       STA ONE",
                "       LDI 3",
                "       STA N",
                "       LDI 0",
                "       STA SUM",
                "loop:  LDA SUM",
                "       CALL addfive   ; A = A + 5",
                "       STA SUM",
                "       LDA N",
                "       DEC",
                "       STA N",
                "       JNZ loop       ; tre volte: SUM = 15",
                "       LDA SUM",
                "       PUSH",
                "       LDI 0",
                "       POP            ; di nuovo 15 (0x0F)",
                "       XOR MASK       ; F0",
                "       AND MASK2      ; 30",
                "       OR ONE         ; 31",
                "       SUB ONE        ; 30",
                "       STA RESULT",
                "       HLT",
                "addfive: ADD FIVE",
                "       RET");
    }

    @Test
    void bothCpusRunTheWholeSetWithSubroutinesAndTheStack()
    {
        Bench h = new Bench(new HarvardCpu("CPU", Families.HC, 10_000, 10_000, 10_000,
                Assembler.assemble(everything(0x0010, 0x8000)).image()));
        Memory low = h.ram(false);
        h.engine.runUntil(200_000_000);
        assertTrue(h.cpu.halted(), "Harvard arrivata a HLT");
        assertNull(h.cpu.fault());
        assertEquals(15L, low.peek(0x0011), "SUM: tre chiamate alla subroutine");
        assertEquals(0x30L, low.peek(0x0016), "RESULT: POP, XOR, AND, OR, SUB");
        assertEquals(0x8000, h.cpu.stack(), "lo stack torna dov'era");
        assertTrue(h.log.events().isEmpty(), h.log.events().toString());

        Bench v = new Bench(new VonNeumannCpu("CPU", Families.HC, 10_000, 10_000, 10_000));
        v.rom(Assembler.assemble(everything(0x8010, 0x0000)).image());          // lo stack parte da FFFF
        Memory high = v.ram(true);
        v.engine.runUntil(400_000_000);
        assertTrue(v.cpu.halted(), "von Neumann arrivata a HLT");
        assertNull(v.cpu.fault());
        assertEquals(15L, high.peek(0x0011));
        assertEquals(0x30L, high.peek(0x0016));
        assertEquals(0x0000, v.cpu.stack());
        assertTrue(v.log.events().isEmpty(), v.log.events().toString());
    }

    @Test
    void callPutsTheReturnAddressInTheStackLowByteFirst()
    {
        Bench h = new Bench(new HarvardCpu("CPU", Families.HC, 10_000, 10_000, 10_000,
                Assembler.assemble("LSP 0x0100\nCALL routine\nHLT\nroutine: HLT").image()));   // CALL a 0003, ritorno a 0006
        Memory ram = h.ram(false);
        h.engine.runUntil(20_000_000);
        assertTrue(h.cpu.halted());
        assertEquals(0x00FE, h.cpu.stack());
        assertEquals(0x06L, ram.peek(0x00FE), "byte basso");
        assertEquals(0x00L, ram.peek(0x00FF), "byte alto");
        assertEquals(7, h.cpu.pc(), "fermo nella subroutine");
        assertEquals(4, h.cpu.cycles(InstructionSet.CALL));
    }

    @Test
    void aDurationShorterThanTheBusCyclesIsRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new TooFast());
    }

    /** Una CPU che vorrebbe una CALL in tre cicli, sul bus: codice, due byte d'indirizzo e due scritture non ci stanno. */
    private static final class TooFast extends Cpu
    {
        TooFast()
        {
            super("CPU", Families.HC, 10_000, 10_000, 10_000, null);
            cable(InstructionSet.CALL, 3);
        }
    }
}
