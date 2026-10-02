package net.gommagomma.sbam.parts.cpu;

import net.gommagomma.sbam.digital.DigitalDevice;
import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.DigitalPort;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.InputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.digital.stage.Timing;
import net.gommagomma.sbam.physics.Tick;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Una CPU generica e semplice: esegue un programma di istruzioni atomiche (macchina ad accumulatore),
 * con il programma al suo interno (come un PIC) e i dati sul bus esterno.
 *
 * Avanza sui fronti di salita del clock esterno (CLK). Un ciclo macchina dura quattro clock, Q1..Q4;
 * ogni istruzione dura un certo numero di cicli, e l'istante dell'istruzione successiva viene da qui:
 * la stessa struttura di uno stimolo, con l'istante calcolato invece che scritto.
 *
 * Pin: A0..A15 (indirizzo), D0..D7 (dati), RD# e WR# (attivi bassi), CLK; stadi della famiglia data.
 */
public final class Cpu extends DigitalDevice
{
    private final Timing timing;

    private final DigitalPin clk;
    private final DigitalPort address;
    private final DigitalPort data;
    private final DigitalPin rd;
    private final DigitalPin wr;
    private final Instruction[] program;

    private int pc = 0;          // l'istruzione presente
    private int next = 0;        // la prossima, decisa da quella presente
    private int cycle = 0;       // ciclo macchina dentro l'istruzione
    private int q = 0;           // fase dentro il ciclo macchina (Q1..Q4 = 0..3)
    private int accumulator = 0;
    private boolean halted = false;
    private long nowPs = 0;

    /**
     * @param propagationPs ritardo di propagazione: dal fronte di CLK alle uscite (indirizzo, dati, RD#, WR#) [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     * @param enablePs      abilitazione: dal fronte di CLK al bus dati pilotato in scrittura [ps]
     *                      (sui datasheet: ten, tPZH/tPZL, tOE)
     * @param disablePs     disabilitazione: dal fronte di CLK all'uscita in alta impedenza [ps]
     *                      (sui datasheet: tdis, tPHZ/tPLZ, tHZ, tOHZ)
     * @param program       le istruzioni, in ordine di indirizzo
     */
    public Cpu(String name, Family family, long propagationPs, long enablePs, long disablePs, Instruction... program)
    {
        super(name);
        this.timing = new Timing(propagationPs, propagationPs, enablePs, disablePs);
        if (program.length == 0) throw new IllegalArgumentException(name + ": un programma vuoto");
        this.program = program.clone();
        Power p = power("VDD", "GND", 10e-12);
        clk = input("CLK", p, family.input());
        address = port("A", p, InputStage.NONE, family.output(), 16);
        data = port("D", p, family, 8);
        rd = output("RD", p, family.output());
        wr = output("WR", p, family.output());
        address.drive(0);
        rd.drive(Drive.H);
        wr.drive(Drive.H);
    }

    public DigitalPin clk()          { return clk; }
    public DigitalPort a()           { return address; }
    public DigitalPort d()           { return data; }
    public DigitalPin rd()           { return rd; }
    public DigitalPin wr()           { return wr; }
    public List<Instruction> program() { return Collections.unmodifiableList(Arrays.asList(program)); }

    /** Lo stato visibile: accumulatore, istruzione presente, fermata. */
    public int accumulator()         { return accumulator; }
    public int pc()                  { return pc; }
    public boolean halted()          { return halted; }

    @Override
    protected void logic(Tick tick)
    {
        if (halted || clk.edge() != Edge.RISING) return;
        nowPs = tick.next();
        Instruction now = program[pc];
        now.phase(cycle, q, this);
        if (++q < 4) return;
        q = 0;
        if (++cycle < now.cycles()) return;
        cycle = 0;
        next = pc + 1;
        now.execute(this);
        pc = next;
        if (pc >= program.length) halted = true;      // finito il programma
    }

    // ------------------------------------------------------------ usati dalle istruzioni

    /** Bus a riposo: dati rilasciati, controlli inattivi (l'indirizzo resta). */
    void idle()
    {
        data.drive(0, 0, nowPs, timing);
    }

    /** Q1 di una lettura: l'indirizzo sul bus, i dati rilasciati. */
    void addressRead(int a)
    {
        address.drive(a, 0xFFFF, nowPs, timing);
        data.drive(0, 0, nowPs, timing);
    }

    /** Q1 di una scrittura: indirizzo e dato sul bus. */
    void addressWrite(int a, int value)
    {
        address.drive(a, 0xFFFF, nowPs, timing);
        data.drive(value, 0xFF, nowPs, timing);
    }

    void read(boolean active)    { rd.drive(active ? Drive.L : Drive.H, nowPs, timing); }
    void write(boolean active)   { wr.drive(active ? Drive.L : Drive.H, nowPs, timing); }

    /** Il byte sul bus dati, letto adesso (i bit X come 0). */
    int sample()                 { return (int) data.read() & 0xFF; }

    void accumulator(int value)  { accumulator = value & 0xFF; }
    boolean zero()               { return accumulator == 0; }
    void jump(int target)        { next = target; }
    void halt()                  { halted = true; next = pc; }
}
