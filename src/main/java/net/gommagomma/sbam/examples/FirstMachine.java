package net.gommagomma.sbam.examples;

import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.instrument.physics.VoltageSignal;
import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.instrument.digital.ContentionSentinel;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.instrument.digital.PortReadSignal;
import net.gommagomma.sbam.instrument.digital.RatingSentinel;
import net.gommagomma.sbam.instrument.digital.UndefinedSentinel;
import net.gommagomma.sbam.instrument.logic.TimingSentinel;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.parts.cpu.Add;
import net.gommagomma.sbam.parts.cpu.Cpu;
import net.gommagomma.sbam.parts.cpu.Inc;
import net.gommagomma.sbam.parts.cpu.Jmp;
import net.gommagomma.sbam.parts.cpu.Lda;
import net.gommagomma.sbam.parts.cpu.Ldi;
import net.gommagomma.sbam.parts.cpu.Sta;
import net.gommagomma.sbam.parts.display.LedBar;
import net.gommagomma.sbam.parts.logic.Buffer;
import net.gommagomma.sbam.parts.logic.Decoder;
import net.gommagomma.sbam.parts.logic.Register;
import net.gommagomma.sbam.parts.memory.Memory;
import net.gommagomma.sbam.parts.passive.BussedNetwork;
import net.gommagomma.sbam.parts.passive.IsolatedNetwork;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.parts.switching.DipSwitch;
import net.gommagomma.sbam.parts.timing.Oscillator;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Wire;

/**
 * La prima macchina: una CPU con il suo bus, una RAM, una porta d'ingresso e una d'uscita.
 *
 *     X1 (4 MHz) -- CPU.CLK
 *     CPU.A0..A15 -- ADDR      CPU.D0..D7 -- DATA      CPU.RD#, CPU.WR#
 *     RAM 62256:   A0..A14 = ADDR0..14,  CE# = A15,  OE# = RD#,  WE# = WR#      (0000-7FFF)
 *     U3A (decoder 2 a 4, G# = WR#):  A15 A14 = 10  ->  Y2  ->  CP del 574 -> LED   (scrittura a 8000)
 *     U3B (decoder 2 a 4, G# = RD#):  A15 A14 = 11  ->  Y3  ->  OE# del 244 <- dip switch (lettura da C000)
 *
 * U3A e U3B sono le due metà di un 74HC139, U6A e U6B quelle di un 74HC244. Il programma (interno alla CPU, come in un PIC) conta in RAM,
 * legge gli interruttori, somma, e mostra il risultato sui LED; a metà prova l'operatore sposta le levette.
 *
 *     0  LDI 00        4  LDA 0000        8  STA 8000     (LED = contatore + interruttori)
 *     1  STA 0000      5  INC             9  JMP 2
 *     2  LDA C000      6  STA 0000
 *     3  STA 0001      7  ADD 0001
 *
 * All'accensione la sentinella di setup e hold segnala un BOING per bit del 574: il suo CP sale insieme
 * all'alimentazione mentre i dati si assestano, e il registro cattura un valore non garantito. È vero
 * anche per il chip reale (il contenuto all'accensione è indefinito); qui lo sovrascrive la prima STA 8000.
 */
public final class FirstMachine
{
    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Supply vcc;
        final Cpu cpu;
        final Memory ram;
        final Register out;
        final Buffer inLow;
        final LedBar leds;
        final Wire clk;

        Circuit()
        {
            sim = new Simulation("first-machine", 1_000);   // 1 ns
            vcc = sim.add(new Supply("VCC", 5.0, 0.001));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 200e-12);
            Wire ground = new Wire("GND", 200e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            Oscillator x1 = sim.add(new Oscillator("X1", Families.HC_GATE, 4e6, 0.5, 500_000));   // oscillatore quarzato, uscita HC
            cpu = sim.add(new Cpu("CPU", Families.HC, 10_000, 10_000, 10_000,
                    new Ldi(0x00), new Sta(0x0000),
                    new Lda(0xC000), new Sta(0x0001), new Lda(0x0000), new Inc(), new Sta(0x0000), new Add(0x0001),
                    new Sta(0x8000), new Jmp(2)));
            ram = sim.add(new Memory("RAM", Families.HCT, 15, 8, 70_000, 35_000, 25_000));   // 62256-70
            Decoder writes = sim.add(new Decoder("U3A", Families.HC_GATE, 2, 1, 0, 11_000));   // le due metà di un 74HC139
            Decoder reads = sim.add(new Decoder("U3B", Families.HC_GATE, 2, 1, 0, 11_000));   // 74HC139
            out = sim.add(new Register("U5", Families.HC, 8, 14_000, 14_000, 12_000, 12_000, 3_000));   // 74HC574
            inLow = sim.add(new Buffer("U6A", Families.HC, 4, 9_000, 14_000, 12_000));   // le due metà di un 74HC244
            Buffer inHigh = sim.add(new Buffer("U6B", Families.HC, 4, 9_000, 14_000, 12_000));   // 74HC244
            // levette ON = 0: l'operatore parte con tutto ON (legge 00), poi apre le quattro basse (legge 0F)
            DipSwitch sw = sim.add(new DipSwitch("SW1", 8, new Stimulus(0, 0xFF), new Stimulus(100_000_000, 0xF0)));
            BussedNetwork pullUps = sim.add(new BussedNetwork("RN1", 8, 10_000));
            IsolatedNetwork series = sim.add(new IsolatedNetwork("RN2", 8, 470));
            leds = sim.add(new LedBar("D1", 8));

            Bus addr = Bus.of("ADDR", 16, 10e-12);
            Bus data = Bus.of("DATA", 8, 10e-12);
            Bus din = Bus.of("DIN", 8, 10e-12);
            Bus q = Bus.of("Q", 8, 10e-12);
            Bus anodes = Bus.of("LED", 8, 5e-12);
            clk = new Wire("CLK", 10e-12);
            Wire rd = new Wire("RD", 10e-12), wr = new Wire("WR", 10e-12);
            Wire outClock = new Wire("OUTCLK", 10e-12), inEnable = new Wire("INEN", 10e-12);

            for (Wire w : new Wire[] { rail }) {
                x1.vdd().connect(w); cpu.vdd().connect(w); ram.vdd().connect(w); writes.vdd().connect(w);
                reads.vdd().connect(w); out.vdd().connect(w); inLow.vdd().connect(w); inHigh.vdd().connect(w); pullUps.common().connect(w);
            }
            for (Wire w : new Wire[] { ground }) {
                x1.gnd().connect(w); cpu.gnd().connect(w); ram.gnd().connect(w); writes.gnd().connect(w);
                reads.gnd().connect(w); out.gnd().connect(w); inLow.gnd().connect(w); inHigh.gnd().connect(w); out.oe().connect(w);
                sw.b().connect(w); leds.k().connect(w);
            }
            x1.out().connect(clk);
            cpu.clk().connect(clk);
            cpu.a().connect(addr);
            cpu.d().connect(data);
            cpu.rd().connect(rd);
            cpu.wr().connect(wr);

            ram.address().connect(addr.slice(0, 15));
            ram.ce().connect(addr.get(15));
            ram.oe().connect(rd);
            ram.we().connect(wr);
            ram.data().connect(data);

            writes.enableLow(0).connect(wr);
            writes.a(0).connect(addr.get(14));
            writes.a(1).connect(addr.get(15));
            writes.y(2).connect(outClock);
            reads.enableLow(0).connect(rd);
            reads.a(0).connect(addr.get(14));
            reads.a(1).connect(addr.get(15));
            reads.y(3).connect(inEnable);

            out.d().connect(data);
            out.cp().connect(outClock);
            out.q().connect(q);
            series.a().connect(q);
            series.b().connect(anodes);
            leds.a().connect(anodes);

            sw.a().connect(din);
            pullUps.r().connect(din);
            inLow.a().connect(din.slice(0, 4));
            inHigh.a().connect(din.slice(4, 8));
            inLow.y().connect(data.slice(0, 4));
            inHigh.y().connect(data.slice(4, 8));
            inLow.oe().connect(inEnable);
            inHigh.oe().connect(inEnable);

            sim.log().echoTo(System.out);
            sim.add(new ContentionSentinel(sim.log()));
            sim.add(new RatingSentinel(sim.log(), 20_000));
            sim.add(new UndefinedSentinel(sim.log(), 500_000));
            sim.add(new TimingSentinel(sim.log()));
            sim.vcd("bus")
                    .add(new LevelSignal(cpu.clk())).add(new PortDriveSignal(cpu.a()))
                    .add(new LevelSignal(ram.oe())).add(new LevelSignal(ram.we()))
                    .add(new PortReadSignal(ram.data()))
                    .add(new LevelSignal(out.cp())).add(new LevelSignal(inLow.oe()))
                    .add(new PortDriveSignal(out.q()));
        }
    }

    /** Per la finestra: il circuito e le sonde, con i nomi da mostrare. */
    private static final class Live implements Setup
    {
        @Override
        public Simulation build(Probes probes)
        {
            Circuit c = new Circuit();
            probes.probe(new LevelSignal(c.cpu.clk()), "clock CPU")
                    .probe(new PortDriveSignal(c.cpu.a()), "indirizzo (CPU)")
                    .probe(new PortReadSignal(c.ram.data()), "dati (letti dalla RAM)")
                    .probe(new LevelSignal(c.ram.oe()), "RD#  lettura")
                    .probe(new LevelSignal(c.ram.we()), "WR#  scrittura")
                    .probe(new LevelSignal(c.out.cp()), "CP del 574 (LED)")
                    .probe(new LevelSignal(c.inLow.oe()), "OE# del 244 (interruttori)")
                    .probe(new PortDriveSignal(c.out.q()), "LED (uscite del 574)")
                    .probe(new VoltageSignal(c.clk), "tensione del clock")
                    .probe(new CurrentSignal(c.vcc.out()), "corrente dall'alimentazione");
            return c.sim;
        }
    }

    public static void main(String[] args)
    {
        if (args.length > 0 && args[0].equals("--live")) {              // a banco: la finestra della simulazione
            new SimulationWindow(new Live()).show();
            return;
        }
        Circuit circuit = new Circuit();
        Simulation sim = circuit.sim;
        LedBar leds = circuit.leds;
        Memory ram = circuit.ram;

        System.out.println("  istante     contatore  interruttori  LED   atteso");
        long lastLeds = -1;
        int errors = 0;
        for (long t = 1_000_000; t <= 200_000_000; t += 1_000_000) {
            sim.runUntil(t);
            long lit = leds.litMask();
            if (lit == lastLeds) continue;
            lastLeds = lit;
            int counter = (int) ram.peek(0x0000), switches = (int) ram.peek(0x0001);
            int expected = (counter + switches) & 0xFF;
            if (lit != expected) errors++;
            System.out.printf("  %6.1f us   %02X         %02X            %02X    %02X %s%n",
                    t / 1e6, counter, switches, lit, expected, lit == expected ? "" : "ERRATO");
        }
        System.out.println("  errori: " + errors + ", eventi: " + sim.log().events().size()
                + " (KABOOM " + sim.log().count(Severity.KABOOM) + ", BOING " + sim.log().count(Severity.BOING) + ")");
        System.out.println("file: " + sim.dir().toAbsolutePath());
        sim.close();
    }
}
