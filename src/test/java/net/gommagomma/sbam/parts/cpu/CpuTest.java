package net.gommagomma.sbam.parts.cpu;

import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.digital.ContentionSentinel;
import net.gommagomma.sbam.parts.memory.Memory;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.parts.timing.Oscillator;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La CPU generica con una RAM sul bus: scrive, rilegge, calcola, e si ferma. */
class CpuTest
{
    @Test
    void runsAProgramAgainstARam()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Wire ground = Ground.of(engine);

        Oscillator x1 = engine.add(new Oscillator("X1", Families.HC_GATE, 4e6, 0.5, 200_000));   // 4 MHz: 1 us per ciclo
        Cpu cpu = engine.add(new Cpu("CPU", Families.HC, 10_000, 10_000, 10_000,
                new Ldi(0x5A), new Sta(0x0010), new Ldi(0), new Lda(0x0010), new Inc(), new Sta(0x0011), new Halt()));
        Memory ram = engine.add(new Memory("RAM", Families.HCT, 15, 8, 70_000, 35_000, 25_000));   // 62256-70
        x1.vdd().connect(rail);  x1.gnd().connect(ground);
        cpu.vdd().connect(rail); cpu.gnd().connect(ground);
        ram.vdd().connect(rail); ram.gnd().connect(ground);
        Wire clk = new Wire("CLK", 10e-12);
        x1.out().connect(clk);
        cpu.clk().connect(clk);
        Bus a = Bus.of("A", 16, 10e-12), d = Bus.of("D", 8, 10e-12);
        cpu.a().connect(a);
        cpu.d().connect(d);
        ram.address().connect(a.slice(0, 15));
        ram.ce().connect(a.get(15));
        ram.data().connect(d);
        Wire rd = new Wire("RD", 10e-12), wr = new Wire("WR", 10e-12);
        cpu.rd().connect(rd);  ram.oe().connect(rd);
        cpu.wr().connect(wr);  ram.we().connect(wr);
        EventLog log = new EventLog();
        engine.add(new ContentionSentinel(log));

        // 1 + 2 + 1 + 2 + 1 + 2 + 1 = 10 cicli macchina, 10 us
        engine.runUntil(15_000_000);
        assertTrue(cpu.halted(), "arrivata a HALT");
        assertEquals(0x5AL, ram.peek(0x0010), "STA scrive in RAM");
        assertEquals(0x5BL, ram.peek(0x0011), "LDA rilegge, INC, STA");
        assertEquals(0x5B, cpu.accumulator());
        assertTrue(log.events().isEmpty(), "nessuno scontro sul bus: " + log.events());
    }

    @Test
    void theTimeOfEachInstructionComesFromTheClockAndItsCycles()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        Wire ground = Ground.of(engine);
        Oscillator x1 = engine.add(new Oscillator("X1", Families.HC_GATE, 4e6, 0.5, 0));
        Cpu cpu = engine.add(new Cpu("CPU", Families.HC, 10_000, 10_000, 10_000, new Nop(), new Jmp(3), new Nop(), new Halt()));
        x1.vdd().connect(rail);  x1.gnd().connect(ground);
        cpu.vdd().connect(rail); cpu.gnd().connect(ground);
        Wire clk = new Wire("CLK", 10e-12);
        x1.out().connect(clk);
        cpu.clk().connect(clk);
        cpu.a().connect(Bus.of("A", 16, 5e-12));
        cpu.d().connect(Bus.of("D", 8, 5e-12));
        cpu.rd().connect(new Wire("RD", 5e-12));
        cpu.wr().connect(new Wire("WR", 5e-12));

        engine.runUntil(1_100_000);
        assertEquals(1, cpu.pc(), "NOP: un ciclo macchina (4 clock, 1 us)");
        engine.runUntil(2_100_000);
        assertEquals(1, cpu.pc(), "JMP dura due cicli: ancora in corso");
        engine.runUntil(3_100_000);
        assertEquals(3, cpu.pc(), "saltato a HALT, senza passare dalla NOP 2");
    }
}
