package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.hardware.cpu.Cpu;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Instrument;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.program.Disassembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo strumento che segue una CPU per la finestra: a ogni istruzione completata annota quando, a quale indirizzo,
 * e che cosa (in testo, con il disassemblatore). Tiene le ultime MAX. Si legge tenendo il lock del runner.
 */
final class CpuWatch implements Instrument
{
    static final int MAX = 2_000;

    private final Cpu cpu;
    private final String label;
    private final List<long[]> executed = new ArrayList<>();      // tempo, indirizzo
    private final List<String> texts = new ArrayList<>();
    private long seen = 0;

    CpuWatch(Cpu cpu, String label)
    {
        this.cpu = cpu;
        this.label = label;
    }

    Cpu cpu()           { return cpu; }
    String label()      { return label; }
    int size()          { return executed.size(); }
    long time(int i)    { return executed.get(i)[0]; }
    int address(int i)  { return (int) executed.get(i)[1]; }
    String text(int i)  { return texts.get(i); }

    @Override
    public void observe(Tick tick, Engine engine)
    {
        if (cpu.retired() == seen) return;
        seen = cpu.retired();
        if (executed.size() == MAX) {
            executed.remove(0);
            texts.remove(0);
        }
        executed.add(new long[] { tick.next(), cpu.lastPc() });
        texts.add(Disassembler.text(cpu.lastInstruction(), cpu.lastOperand()));
    }
}
