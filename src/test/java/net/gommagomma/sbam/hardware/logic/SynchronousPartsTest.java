package net.gommagomma.sbam.hardware.logic;

import net.gommagomma.sbam.hardware.logic.Register;
import net.gommagomma.sbam.hardware.logic.Counter;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.instrument.logic.TimingSentinel;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.hardware.timing.Oscillator;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 74HC574 e 74HC161: catturano sui fronti, e la sentinella verifica setup e hold. */
class SynchronousPartsTest
{
    private Engine engine;
    private Wire rail;

    private void bench()
    {
        engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
    }

    private void power(Pin vdd, Pin gnd)
    {
        vdd.connect(rail);
        gnd.connect(Ground.of(engine));
    }

    private Source source(String name, Pin input, Source s)
    {
        engine.add(s);
        power(s.vdd, s.gnd);
        Wire w = new Wire(name, 5e-12);
        s.out.connect(w);
        input.connect(w);
        return s;
    }

    @Test
    void theRegisterCapturesOnTheClockEdgeAndOeOnlyOpensTheOutputs()
    {
        bench();
        Register u = engine.add(new Register("U", Families.HC, 8, 14_000, 14_000, 12_000, 12_000, 3_000));   // 74HC574
        power(u.vdd(), u.gnd());
        WordSource data = engine.add(new WordSource("DATA", Families.HC, 8).at(0, 0x3C, 0xFF).at(300_000, 0xA5, 0xFF));
        power(data.vdd, data.gnd);
        Bus d = Bus.of("D", 8, 5e-12);
        data.port.connect(d);
        u.d().connect(d);
        u.q().connect(Bus.of("Q", 8, 5e-12));
        source("CP", u.cp(), new Source("SCP", Families.HC_TRISTATE).at(0, Drive.L).at(200_000, Drive.H).at(400_000, Drive.L));
        source("OE", u.oe(), new Source("SOE", Families.HC_TRISTATE).at(0, Drive.H).at(500_000, Drive.L));
        EventLog log = new EventLog();
        engine.add(new TimingSentinel(log));

        engine.runUntil(150_000);
        assertEquals(0, u.stored(), "nessun fronte ancora");
        engine.runUntil(450_000);
        assertEquals(0x3C, u.stored(), "catturato a 200 ns; il cambio di D a 300 ns non conta");
        assertEquals(0xFF, u.q().released(), "OE# alto: uscite rilasciate");
        engine.runUntil(600_000);
        assertEquals(0, u.q().released());
        assertEquals(0x3C, u.q().driven(), "OE# basso: il registro sulle uscite");
        assertTrue(log.events().isEmpty(), "dati stabili: " + log.events());
    }

    @Test
    void dataChangingTooCloseToTheEdgeIsABoing()
    {
        bench();
        Register u = engine.add(new Register("U", Families.HC, 8, 14_000, 14_000, 12_000, 12_000, 3_000));   // 74HC574
        power(u.vdd(), u.gnd());
        WordSource data = engine.add(new WordSource("DATA", Families.HC, 8).at(0, 0x00, 0xFF).at(195_000, 0xFF, 0xFF));
        power(data.vdd, data.gnd);
        Bus d = Bus.of("D", 8, 5e-12);
        data.port.connect(d);
        u.d().connect(d);
        source("CP", u.cp(), new Source("SCP", Families.HC_TRISTATE).at(0, Drive.L).at(200_000, Drive.H));
        source("OE", u.oe(), new Source("SOE", Families.HC_TRISTATE).at(0, Drive.L));
        EventLog log = new EventLog();
        engine.add(new TimingSentinel(log));
        engine.runUntil(300_000);

        assertEquals(8, log.count(Severity.BOING), "un BOING per bit: " + log.events());
        assertTrue(log.events().get(0).message().contains("setup violato"), log.events().get(0).message());
    }

    @Test
    void theCounterCountsWrapsAndCarries()
    {
        bench();
        Counter c = engine.add(new Counter("U", Families.HC_GATE, 4, 16_000, 12_000, 3_000));   // 74HC161
        power(c.vdd(), c.gnd());
        Oscillator x = engine.add(new Oscillator("X", Families.HC_GATE, 10e6, 0.5, 100_000));
        power(x.vdd(), x.gnd());
        Wire clk = new Wire("CLK", 10e-12);
        x.out().connect(clk);
        c.cp().connect(clk);
        for (Pin p : new Pin[] { c.pe(), c.cep(), c.cet() }) p.connect(rail);
        source("MR", c.mr(), new Source("SMR", Families.HC_TRISTATE).at(0, Drive.L).at(50_000, Drive.H));
        c.d().connect(Ground.of(engine));    // D non usati: a massa
        c.q().connect(Bus.of("Q", 4, 5e-12));
        c.tc().connect(new Wire("TC", 5e-12));
        EventLog log = new EventLog();
        engine.add(new TimingSentinel(log));

        // primo fronte a 100 ns, poi uno ogni 100 ns: a 100 + k*100 + 50 ns ne sono passati k + 1
        engine.runUntil(100_000 + 5 * 100_000 + 50_000);
        assertEquals(6, c.count(), "sei fronti");
        assertEquals(6, c.q().driven());
        engine.runUntil(100_000 + 14 * 100_000 + 50_000);
        assertEquals(15, c.count(), "quindici fronti");
        assertEquals(Drive.H, c.tc().driven(), "riporto a 15");
        engine.runUntil(100_000 + 15 * 100_000 + 50_000);
        assertEquals(0, c.count(), "il sedicesimo: ricomincia da zero");
        assertEquals(Drive.L, c.tc().driven());
        assertTrue(log.events().isEmpty(), "controlli fermi, nessuna violazione: " + log.events());
    }

    @Test
    void aRegisterCanHaveAnyWidth()
    {
        bench();
        Register u = engine.add(new Register("R3", Families.HC, 3, 5_000, 5_000, 5_000, 5_000, 1_000));
        power(u.vdd(), u.gnd());
        WordSource data = engine.add(new WordSource("DATA", Families.HC, 3).at(0, 0b101, 0b111));
        power(data.vdd, data.gnd);
        Bus d = Bus.of("D", 3, 5e-12);
        data.port.connect(d);
        u.d().connect(d);
        u.q().connect(Bus.of("Q", 3, 5e-12));
        u.oe().connect(Ground.of(engine));
        source("CP", u.cp(), new Source("SCP", Families.HC_TRISTATE).at(0, Drive.L).at(100_000, Drive.H));
        engine.runUntil(200_000);
        assertEquals(0b101, u.stored(), "un registro da 3 bit, senza fingere un chip");
        assertEquals(0b101, u.q().driven());
    }
}
