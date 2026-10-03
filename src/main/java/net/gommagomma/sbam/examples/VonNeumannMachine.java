package net.gommagomma.sbam.examples;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import net.gommagomma.sbam.Simulation;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.gui.Probes;
import net.gommagomma.sbam.gui.Setup;
import net.gommagomma.sbam.gui.SimulationWindow;
import net.gommagomma.sbam.hardware.cpu.Cpu;
import net.gommagomma.sbam.hardware.cpu.VonNeumannCpu;
import net.gommagomma.sbam.hardware.display.LedBar;
import net.gommagomma.sbam.hardware.logic.Buffer;
import net.gommagomma.sbam.hardware.logic.Decoder;
import net.gommagomma.sbam.hardware.logic.Register;
import net.gommagomma.sbam.hardware.memory.Memory;
import net.gommagomma.sbam.hardware.memory.Rom;
import net.gommagomma.sbam.hardware.passive.BussedNetwork;
import net.gommagomma.sbam.hardware.passive.IsolatedNetwork;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.hardware.switching.DipSwitch;
import net.gommagomma.sbam.hardware.timing.Oscillator;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.instrument.digital.ContentionSentinel;
import net.gommagomma.sbam.instrument.digital.DriveSignal;
import net.gommagomma.sbam.instrument.digital.LevelSignal;
import net.gommagomma.sbam.instrument.digital.PortDriveSignal;
import net.gommagomma.sbam.instrument.digital.PortReadSignal;
import net.gommagomma.sbam.instrument.digital.RatingSentinel;
import net.gommagomma.sbam.instrument.digital.UndefinedSentinel;
import net.gommagomma.sbam.instrument.logic.TimingSentinel;
import net.gommagomma.sbam.instrument.physics.CurrentSignal;
import net.gommagomma.sbam.logic.Stimulus;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Wire;
import net.gommagomma.sbam.program.Assembler;
import net.gommagomma.sbam.program.image.IntelHex;
import net.gommagomma.sbam.program.image.MemoryImage;

/**
 * La macchina von Neumann: una CPU che legge il programma da una ROM sullo stesso bus dei dati.
 *
 *     X1 (4 MHz) -- CPU.CLK
 *     CPU.A0..A15 -- ADDR      CPU.D0..D7 -- DATA      CPU.RD#, CPU.WR#
 *     ROM 27C256:  A0..A14 = ADDR0..14,  CE# = A15,  OE# = RD#                         (0000-7FFF, il programma)
 *     U4A (decoder 2 a 4, G# a massa):  A15 A14 = 10  ->  Y2  ->  CE# della RAM
 *     RAM 62256:   A0..A13 = ADDR0..13,  A14 a massa,  OE# = RD#,  WE# = WR#           (8000-BFFF)
 *     U3A (decoder 2 a 4, G# = WR#):  A15 A14 = 11  ->  Y3  ->  CP del 574 -> LED       (scrittura a C000)
 *     U3B (decoder 2 a 4, G# = RD#):  A15 A14 = 11  ->  Y3  ->  OE# del 244 <- dip switch (lettura da C000)
 *
 * U3A e U3B sono le due metà di un 74HC139, U4A metà di un altro; U6A e U6B le due metà di un 74HC244.
 * Il programma è von-neumann-machine.asm (nelle risorse, accanto a questa classe): l'assemblatore lo traduce
 * nell'immagine della ROM, che finisce anche in rom.hex, pronta per un programmatore di EPROM vero.
 * A metà prova l'operatore sposta le levette.
 *
 * Rispetto alla macchina Harvard ogni istruzione costa di più: il codice e gli operandi passano dal bus.
 * Il giro del programma, con la chiamata alla subroutine, dura 43 cicli macchina (43 us) invece di 15.
 */
public final class VonNeumannMachine
{
    /** Il programma della ROM, dalle risorse. */
    private static String program()
    {
        InputStream in = VonNeumannMachine.class.getResourceAsStream("von-neumann-machine.asm");
        if (in == null) throw new IllegalStateException("von-neumann-machine.asm non trovato nelle risorse");
        try {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Il circuito, costruito da capo ogni volta: per la prova da console e per la finestra (a ogni reset). */
    private static final class Circuit
    {
        final Simulation sim;
        final Supply vcc;
        final Cpu cpu;
        final Rom rom;
        final Memory ram;
        final Register out;
        final Buffer inLow;
        final Decoder select;
        final LedBar leds;
        final Wire clk;

        Circuit()
        {
            sim = new Simulation("von-neumann-machine", 1_000);   // 1 ns
            vcc = sim.add(new Supply("VCC", 5.0, 0.001));
            Supply gnd = sim.add(new Supply("GND", 0.0, 0.001));
            Wire rail = new Wire("+5V", 200e-12);
            Wire ground = new Wire("GND", 200e-12);
            vcc.out().connect(rail);
            gnd.out().connect(ground);

            MemoryImage image = Assembler.assemble("von-neumann-machine.asm", program()).image();
            IntelHex.write(image, sim.file("rom.hex"));

            Oscillator x1 = sim.add(new Oscillator("X1", Families.HC_GATE, 4e6, 0.5, 500_000));   // oscillatore quarzato, uscita HC
            cpu = sim.add(new VonNeumannCpu("CPU", Families.HC, 10_000, 10_000, 10_000));
            rom = sim.add(new Rom("ROM", Families.HC, 15, 8, 150_000, 75_000, 50_000, image));   // 27C256-15
            ram = sim.add(new Memory("RAM", Families.HCT, 15, 8, 70_000, 35_000, 25_000));   // 62256-70
            select = sim.add(new Decoder("U4A", Families.HC_GATE, 2, 1, 0, 11_000));   // metà di un 74HC139
            Decoder writes = sim.add(new Decoder("U3A", Families.HC_GATE, 2, 1, 0, 11_000));   // le due metà di un 74HC139
            Decoder reads = sim.add(new Decoder("U3B", Families.HC_GATE, 2, 1, 0, 11_000));
            out = sim.add(new Register("U5", Families.HC, 8, 14_000, 14_000, 12_000, 12_000, 3_000));   // 74HC574
            inLow = sim.add(new Buffer("U6A", Families.HC, 4, 9_000, 14_000, 12_000));   // le due metà di un 74HC244
            Buffer inHigh = sim.add(new Buffer("U6B", Families.HC, 4, 9_000, 14_000, 12_000));
            // levette ON = 0: l'operatore parte con tutto ON (legge 00), poi apre le quattro basse (legge 0F)
            DipSwitch sw = sim.add(new DipSwitch("SW1", 8, new Stimulus(0, 0xFF), new Stimulus(200_000_000, 0xF0)));
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
            Wire ramSelect = new Wire("RAMCE", 10e-12);
            Wire outClock = new Wire("OUTCLK", 10e-12), inEnable = new Wire("INEN", 10e-12);

            for (Wire w : new Wire[] { rail }) {
                x1.vdd().connect(w); cpu.vdd().connect(w); rom.vdd().connect(w); ram.vdd().connect(w); select.vdd().connect(w);
                writes.vdd().connect(w); reads.vdd().connect(w); out.vdd().connect(w); inLow.vdd().connect(w); inHigh.vdd().connect(w);
                pullUps.common().connect(w);
            }
            for (Wire w : new Wire[] { ground }) {
                x1.gnd().connect(w); cpu.gnd().connect(w); rom.gnd().connect(w); ram.gnd().connect(w); select.gnd().connect(w);
                writes.gnd().connect(w); reads.gnd().connect(w); out.gnd().connect(w); inLow.gnd().connect(w); inHigh.gnd().connect(w);
                out.oe().connect(w); select.enableLow(0).connect(w); ram.address().get(14).connect(w);
                sw.b().connect(w); leds.k().connect(w);
            }
            x1.out().connect(clk);
            cpu.clk().connect(clk);
            cpu.a().connect(addr);
            cpu.d().connect(data);
            cpu.rd().connect(rd);
            cpu.wr().connect(wr);

            rom.address().connect(addr.slice(0, 15));
            rom.ce().connect(addr.get(15));
            rom.oe().connect(rd);
            rom.data().connect(data);

            select.a(0).connect(addr.get(14));
            select.a(1).connect(addr.get(15));
            select.y(2).connect(ramSelect);
            ram.address().slice(0, 14).connect(addr.slice(0, 14));
            ram.ce().connect(ramSelect);
            ram.oe().connect(rd);
            ram.we().connect(wr);
            ram.data().connect(data);

            writes.enableLow(0).connect(wr);
            writes.a(0).connect(addr.get(14));
            writes.a(1).connect(addr.get(15));
            writes.y(3).connect(outClock);
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
                    .add(new LevelSignal(cpu.clk())).add(new PortDriveSignal(cpu.a())).add(new PortReadSignal(cpu.d()))
                    .add(new DriveSignal(cpu.rd())).add(new DriveSignal(cpu.wr()))
                    .add(new LevelSignal(rom.ce())).add(new LevelSignal(ram.ce()))
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
            probes.cpu(c.cpu, "CPU")
                    .probe(new LevelSignal(c.cpu.clk()), "clock CPU")
                    .probe(new PortDriveSignal(c.cpu.a()), "indirizzo (CPU)")
                    .probe(new PortReadSignal(c.cpu.d()), "bus dati")
                    .probe(new DriveSignal(c.cpu.rd()), "RD#  lettura")
                    .probe(new DriveSignal(c.cpu.wr()), "WR#  scrittura")
                    .probe(new LevelSignal(c.rom.ce()), "CE# della ROM (programma)")
                    .probe(new LevelSignal(c.ram.ce()), "CE# della RAM")
                    .probe(new LevelSignal(c.out.cp()), "CP del 574 (LED)")
                    .probe(new LevelSignal(c.inLow.oe()), "OE# del 244 (interruttori)")
                    .probe(new PortDriveSignal(c.out.q()), "LED (uscite del 574)")
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
        for (long t = 1_000_000; t <= 400_000_000; t += 1_000_000) {
            sim.runUntil(t);
            long lit = leds.litMask();
            if (lit == lastLeds) continue;
            lastLeds = lit;
            int counter = (int) ram.peek(0x0000), switches = (int) ram.peek(0x0001);    // 8000 e 8001 per la CPU
            int expected = (counter + switches) & 0xFF;
            if (lit != expected) errors++;
            System.out.printf("  %6.1f us   %02X         %02X            %02X    %02X %s%n",
                    t / 1e6, counter, switches, lit, expected, lit == expected ? "" : "ERRATO");
        }
        System.out.println("  errori: " + errors + ", eventi: " + sim.log().events().size()
                + " (KABOOM " + sim.log().count(Severity.KABOOM) + ", BOING " + sim.log().count(Severity.BOING) + ")"
                + (circuit.cpu.fault() != null ? ", CPU: " + circuit.cpu.fault() : ""));
        System.out.println("file: " + sim.dir().toAbsolutePath() + " (con rom.hex, l'immagine della ROM)");
        sim.close();
    }
}
