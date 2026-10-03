package net.gommagomma.sbam.hardware.cpu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import net.gommagomma.sbam.program.Access;
import net.gommagomma.sbam.program.Instruction;
import net.gommagomma.sbam.program.Registers;
import net.gommagomma.sbam.program.Transfer;
import net.gommagomma.sbam.program.image.MemoryImage;

/**
 * Il nucleo di una CPU semplice, macchina ad accumulatore da 8 bit con indirizzi da 16: il bus, i registri,
 * i cicli macchina, e la tabella delle istruzioni cablate.
 *
 * La tabella dice tutto e solo quello che questa CPU sa eseguire: quali istruzioni del set (InstructionSet,
 * nel package program) e quanti cicli macchina costa ciascuna qui. Il codice operativo non si riscrive: viene
 * dall'istruzione. Un codice che la tabella non ha ferma la CPU con un guasto (fault).
 *
 * Il programma sta in una memoria interna (Harvard, come un PIC: codice e operandi in un ciclo, sul bus solo
 * i dati) oppure sul bus, come i dati (von Neumann: un ciclo di bus per il codice e uno per ogni byte di
 * operando). Poi un ciclo di bus per ogni trasferimento di dati dell'istruzione (all'operando, o nello stack);
 * i cicli che restano fino alla durata della tabella sono interni.
 *
 * Avanza sui fronti di salita del clock esterno (CLK). Un ciclo macchina dura quattro clock, Q1..Q4:
 *  - interno:  Q1 dati rilasciati (l'indirizzo resta);
 *  - lettura:  Q1 indirizzo sul bus, dati rilasciati;  Q2 RD# basso;  Q4 campiona i dati, RD# alto;
 *  - scrittura: Q1 indirizzo e dato sul bus;  Q2 WR# basso;  Q4 WR# alto (il dato resta fino al ciclo dopo).
 * All'accensione il programma parte da 0000.
 *
 * Pin: A0..A15 (indirizzo), D0..D7 (dati), RD# e WR# (attivi bassi), CLK; stadi della famiglia data.
 */
public abstract class Cpu extends DigitalDevice
{
    /** A che cosa serve il ciclo macchina in corso. */
    private enum Purpose { FETCH, OPCODE, OPERAND, DATA, INTERNAL }

    private final Timing timing;
    private final Registers registers = new State();
    private final MemoryImage internal;                    // null: il programma è sul bus
    private final Opcode[] table = new Opcode[256];                // indicizzata dal codice operativo
    private final List<Opcode> cabled = new ArrayList<>();          // le stesse righe, nell'ordine in cui si cablano

    private final DigitalPin clk;
    private final DigitalPort address;
    private final DigitalPort data;
    private final DigitalPin rd;
    private final DigitalPin wr;

    // i registri
    private int pc = 0;                 // l'indirizzo dell'istruzione presente
    private int next = 0;               // l'indirizzo della prossima, deciso da quella presente
    private int accumulator = 0;
    private int stack = 0;              // lo stack cresce verso il basso: il primo byte va a FFFF
    private boolean halted = false;
    private String fault = null;

    // l'istruzione in corso
    private Opcode current = null;
    private int operand = 0, operandsRead = 0, done = 0;
    private List<Transfer> transfers = null;         // calcolati al primo ciclo di dati
    private int[] read = new int[0];                   // i byte letti nei trasferimenti
    private int transferred = 0;

    // l'ultima istruzione completata, per chi osserva
    private long retired = 0;
    private int lastPc = 0, lastOperand = 0;
    private Instruction last = null;

    // il ciclo macchina in corso
    private int q = 0;                  // Q1..Q4 = 0..3
    private Access kind = Access.NONE;
    private Purpose purpose = Purpose.FETCH;
    private int busAddress = 0;
    private int busValue = 0;

    /**
     * @param propagationPs ritardo di propagazione: dal fronte di CLK alle uscite (indirizzo, dati, RD#, WR#) [ps]
     *                      (sui datasheet: tpd, tPLH/tPHL, tPD)
     * @param enablePs      abilitazione: dal fronte di CLK al bus dati pilotato in scrittura [ps]
     *                      (sui datasheet: ten, tPZH/tPZL, tOE)
     * @param disablePs     disabilitazione: dal fronte di CLK all'uscita in alta impedenza [ps]
     *                      (sui datasheet: tdis, tPHZ/tPLZ, tHZ, tOHZ)
     * @param internal      la memoria di programma interna; null se il programma si legge dal bus
     */
    protected Cpu(String name, Family family, long propagationPs, long enablePs, long disablePs, MemoryImage internal)
    {
        super(name);
        if (internal != null && internal.end() > 0x10000) throw new IllegalArgumentException(name + ": il programma supera 64K");
        this.timing = new Timing(propagationPs, propagationPs, enablePs, disablePs);
        this.internal = internal;
        Power p = power(10e-12);
        clk = input("CLK", p, family.input());
        address = port("A", p, InputStage.NONE, family.output(), 16);
        data = port("D", p, family, 8);
        rd = output("RD", p, family.output());
        wr = output("WR", p, family.output());
        address.drive(0);
        rd.drive(Drive.H);
        wr.drive(Drive.H);
    }

    /**
     * Cabla un'istruzione del set, con la sua durata in questa CPU. La durata non può essere più breve dei
     * cicli che l'istruzione richiede comunque: la lettura (dal bus o interna) e i trasferimenti di dati.
     */
    protected final void cable(Instruction instruction, int machineCycles)
    {
        int code = instruction.opcode();
        if (table[code] != null) throw new IllegalArgumentException(name() + ": " + instruction + " già cablata");
        int fetch = internal != null ? 1 : 1 + instruction.operandBytes();
        int minimum = fetch + instruction.dataCycles();
        if (machineCycles < minimum) {
            throw new IllegalArgumentException(name() + ": " + instruction + " richiede almeno " + minimum + " cicli, non " + machineCycles);
        }
        Opcode row = new Opcode(instruction, machineCycles);
        table[code] = row;
        cabled.add(row);
    }

    public final DigitalPin clk()          { return clk; }
    public final DigitalPort a()           { return address; }
    public final DigitalPort d()           { return data; }
    public final DigitalPin rd()           { return rd; }
    public final DigitalPin wr()           { return wr; }

    /** Le istruzioni che questa CPU sa eseguire, nell'ordine in cui le cabla. */
    public final List<Instruction> instructions()
    {
        List<Instruction> list = new ArrayList<>(cabled.size());
        for (Opcode row : cabled) list.add(row.instruction);
        return Collections.unmodifiableList(list);
    }

    /** Quanti cicli macchina costa un'istruzione qui (0 se non è cablata). */
    public final int cycles(Instruction instruction)
    {
        Opcode row = table[instruction.opcode()];
        return row != null && row.instruction == instruction ? row.cycles : 0;
    }

    /** Lo stato visibile: accumulatore, puntatore allo stack, indirizzo dell'istruzione presente, fermata. */
    public final int accumulator()         { return accumulator; }
    public final int stack()               { return stack; }
    public final int pc()                  { return pc; }
    public final boolean halted()          { return halted; }
    /** L'istruzione in corso; null mentre se ne legge il codice. */
    public final Instruction instruction() { return current == null ? null : current.instruction; }
    /** L'operando dell'istruzione in corso, per quanto letto finora. */
    public final int operand()             { return operand; }
    /** Il ciclo macchina dentro l'istruzione in corso (da 0) e la fase dentro il ciclo (Q1..Q4 = 0..3). */
    public final int machineCycle()        { return done; }
    public final int quarter()             { return q; }

    /** Quante istruzioni ha completato; le tre che seguono descrivono l'ultima. */
    public final long retired()            { return retired; }
    public final Instruction lastInstruction() { return last; }
    public final int lastPc()              { return lastPc; }
    public final int lastOperand()         { return lastOperand; }

    /** Perché si è fermata per un errore (un codice che non sa eseguire); null se non è successo. */
    @Override
    public final String fault()            { return fault; }

    // ------------------------------------------------------------ il ciclo

    @Override
    protected final void logic(Tick tick)
    {
        if (halted || clk.edge() != Edge.RISING) return;
        if (q == 0) plan();
        drive(tick.next());
        if (++q < 4) return;
        q = 0;
        finish();
    }

    /** All'inizio di un ciclo macchina: che ciclo è, e a quale indirizzo. */
    private void plan()
    {
        kind = Access.NONE;
        if (current == null) {
            if (internal == null) {
                kind = Access.READ;
                busAddress = pc;
                purpose = Purpose.OPCODE;
            } else {
                purpose = Purpose.FETCH;           // codice e operandi dalla memoria interna, in un ciclo
                if (decode(internal.read(pc, 0xFF))) {
                    for (int i = 0; i < current.instruction.operandBytes(); i++) operand |= internal.read((pc + 1 + i) & 0xFFFF, 0xFF) << (8 * i);
                    operandsRead = current.instruction.operandBytes();
                }
            }
        } else if (operandsRead < current.instruction.operandBytes()) {
            kind = Access.READ;
            busAddress = (pc + 1 + operandsRead) & 0xFFFF;
            purpose = Purpose.OPERAND;
        } else if (transferred < current.instruction.dataCycles()) {
            if (transfers == null) transfers = current.instruction.transfers(registers, operand);
            Transfer t = transfers.get(transferred);
            kind = t.access();
            busAddress = t.address();
            busValue = t.value();
            purpose = Purpose.DATA;
        } else {
            purpose = Purpose.INTERNAL;
        }
    }

    /** Che cosa fa sui pin nella fase q del ciclo; le uscite cambiano dall'istante dato, con i loro ritardi. */
    private void drive(long nowPs)
    {
        switch (kind) {
            case NONE:
                if (q == 0) data.drive(0, 0, nowPs, timing);
                break;
            case READ:
                if (q == 0) {
                    address.drive(busAddress, 0xFFFF, nowPs, timing);
                    data.drive(0, 0, nowPs, timing);
                }
                if (q == 1) rd.drive(Drive.L, nowPs, timing);
                if (q == 3) rd.drive(Drive.H, nowPs, timing);
                break;
            case WRITE:
                if (q == 0) {
                    address.drive(busAddress, 0xFFFF, nowPs, timing);
                    data.drive(busValue, 0xFF, nowPs, timing);
                }
                if (q == 1) wr.drive(Drive.L, nowPs, timing);
                if (q == 3) wr.drive(Drive.H, nowPs, timing);
                break;
            default:
                throw new IllegalStateException("ciclo non previsto: " + kind);
        }
    }

    /** Alla fine di un ciclo macchina: che cosa ha portato, e se l'istruzione è finita. */
    private void finish()
    {
        int sample = kind == Access.READ ? (int) data.read() & 0xFF : 0;    // campionato in Q4 (i bit X come 0)
        switch (purpose) {
            case FETCH:
            case INTERNAL:
                break;
            case OPCODE:
                decode(sample);
                break;
            case OPERAND:
                operand |= sample << (8 * operandsRead);
                operandsRead++;
                break;
            case DATA:
                read[transferred++] = sample;
                break;
            default:
                throw new IllegalStateException("ciclo non previsto: " + purpose);
        }
        if (halted || current == null) return;
        if (++done < current.cycles) return;
        next = following();
        current.instruction.execute(registers, operand, read);
        retired++;
        last = current.instruction;
        lastPc = pc;
        lastOperand = operand;
        pc = next;
        current = null;
        operand = operandsRead = done = transferred = 0;
        transfers = null;
    }

    /** L'istruzione di un codice, dalla tabella; un codice che non c'è ferma la CPU. Vero se l'ha trovata. */
    private boolean decode(int opcode)
    {
        current = table[opcode & 0xFF];
        if (current != null) {
            if (transfers != null) throw new IllegalStateException("trasferimenti rimasti da un'istruzione prima");
            read = new int[current.instruction.dataCycles()];
            return true;
        }
        halted = true;
        fault = String.format("codice sconosciuto %02X a %04X", opcode, pc);
        return false;
    }

    /** L'indirizzo dell'istruzione dopo quella in corso. */
    private int following()
    {
        return (pc + 1 + current.instruction.operandBytes()) & 0xFFFF;
    }

    // ------------------------------------------------------------ la tabella

    /** Una riga della tabella cablata: un'istruzione del set e quanti cicli macchina costa in questa CPU. */
    private static final class Opcode
    {
        final Instruction instruction;
        final int cycles;

        Opcode(Instruction instruction, int cycles)
        {
            this.instruction = instruction;
            this.cycles = cycles;
        }
    }

    // ------------------------------------------------------------ il modello del programmatore

    /** Quello che vedono le istruzioni: i registri, senza i pin. */
    private final class State implements Registers
    {
        @Override public int accumulator()               { return accumulator; }
        @Override public void accumulator(int value)     { accumulator = value & 0xFF; }
        @Override public boolean zero()                  { return accumulator == 0; }
        @Override public int stack()                     { return stack; }
        @Override public void stack(int address)         { stack = address & 0xFFFF; }
        @Override public int following()                 { return Cpu.this.following(); }
        @Override public void jump(int address)          { next = address & 0xFFFF; }
        @Override public void halt()                     { halted = true; next = pc; }
    }
}
