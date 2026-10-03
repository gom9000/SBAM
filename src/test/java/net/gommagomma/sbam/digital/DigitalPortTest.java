package net.gommagomma.sbam.digital;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSink;
import net.gommagomma.sbam.fixtures.DigitalFixtures.WordSource;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.hardware.passive.Resistor;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Bus;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Porte digitali: la parola letta e pilotata su un bus, con le stesse regole dei singoli pin. */
class DigitalPortTest
{
    private static Wire rail(Engine engine)
    {
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        return rail;
    }

    @Test
    void aWordTravelsOnTheBus()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        WordSource cpu = engine.add(new WordSource("CPU", Families.HC, 8)
                .at(0, 0xA5, 0xFF).at(100_000, 0x3C, 0xFF));
        WordSink ram = engine.add(new WordSink("RAM", Families.HC_IN, 8));
        cpu.vdd.connect(rail);
        cpu.gnd.connect(Ground.of(engine));
        ram.vdd.connect(rail);
        ram.gnd.connect(Ground.of(engine));
        Bus d = Bus.of("D", 8, 10e-12);
        cpu.port.connect(d);
        ram.port.connect(d);

        engine.runUntil(50_000);
        assertEquals(0xA5, ram.port.read());
        assertEquals(0, ram.port.undefined());
        engine.runUntil(150_000);
        assertEquals(0x3C, ram.port.read());
        assertEquals(0, ram.port.changed(), "niente fronti a bus fermo");
    }

    @Test
    void disabledBitsAreReleasedAndUndefinedBitsAreReportedNotRefused()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        WordSource cpu = engine.add(new WordSource("CPU", Families.HC, 4).at(0, 0b0101, 0b0011));
        WordSink in = engine.add(new WordSink("IN", Families.HC_IN, 4));
        cpu.vdd.connect(rail);
        cpu.gnd.connect(Ground.of(engine));
        in.vdd.connect(rail);
        in.gnd.connect(Ground.of(engine));
        Bus d = Bus.of("D", 4, 10e-12);
        cpu.port.connect(d);
        in.port.connect(d);
        // bit 2 tenuto a metà da un partitore: X per un ingresso HC
        Resistor up = engine.add(new Resistor("RU", 1_000));
        Resistor down = engine.add(new Resistor("RD", 1_000));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.1));
        Wire ground = new Wire("GND", 10e-12);
        gnd.out().connect(ground);
        up.a().connect(rail);
        up.b().connect(d.get(2));
        down.a().connect(d.get(2));
        down.b().connect(ground);

        engine.runUntil(50_000);
        assertEquals(Drive.H, cpu.port.get(0).driven());
        assertEquals(Drive.L, cpu.port.get(1).driven());
        assertEquals(Drive.Z, cpu.port.get(2).driven(), "bit non abilitato: rilasciato");
        assertEquals(Drive.Z, cpu.port.get(3).driven());
        assertEquals(0b0100, in.port.undefined(), "il bit 2 legge X: segnalato, non rifiutato");
        assertEquals(0b0001, in.port.read() & 0b0011);
    }

    @Test
    void aTotemPolePortCannotBeReleased()
    {
        WordSource out = new WordSource("OUT", new Family("HC totem", Families.HC_IN, Families.HC_TOTEM), 2);
        assertThrows(IllegalArgumentException.class, out.port::release, "uno stadio totem-pole non rilascia");
    }
}
