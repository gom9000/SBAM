package net.gommagomma.sbam.digital;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Sink;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.hardware.passive.Resistor;
import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'alimentazione completa: la corrente di un'uscita bassa rientra dal pin GND, e la massa conta. */
class PowerTest
{
    @Test
    void aLowOutputReturnsItsCurrentThroughGnd()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12), ground = new Wire("GND", 100e-12);
        vcc.out().connect(rail);
        gnd.out().connect(ground);
        Source src = engine.add(new Source("U", Families.HC_TRISTATE).at(0, Drive.L));
        Resistor pullUp = engine.add(new Resistor("R", 1_000));
        Wire line = new Wire("LINE", 10e-12);
        src.vdd.connect(rail);
        src.gnd.connect(ground);
        src.out.connect(line);
        pullUp.a().connect(rail);
        pullUp.b().connect(line);
        engine.runUntil(100_000);

        double sunk = -src.out.current();                 // corrente che entra nell'uscita
        assertTrue(sunk > 4e-3, "l'uscita bassa assorbe ~(5 V)/(1 kohm + 55 ohm): " + sunk);
        assertEquals(sunk, src.gnd.current(), 1e-6, "e la restituisce dal suo pin GND");
        assertEquals(-sunk, gnd.out().current(), 1e-5, "fino all'alimentatore di massa");
    }

    @Test
    void aResistiveGroundLiftsTheLowLevel()
    {
        Engine engine = new Engine(1_000);
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.001));
        Supply gnd = engine.add(new Supply("GND", 0.0, 0.001));
        Wire rail = new Wire("+5V", 100e-12), ground = new Wire("GND", 100e-12), localGround = new Wire("GND_U", 10e-12);
        vcc.out().connect(rail);
        gnd.out().connect(ground);
        Resistor groundTrace = engine.add(new Resistor("RG", 50));   // un ritorno di massa pessimo
        groundTrace.a().connect(localGround);
        groundTrace.b().connect(ground);

        Source src = engine.add(new Source("U", Families.HC_TRISTATE).at(0, Drive.L));
        Resistor load = engine.add(new Resistor("RL", 220));
        Sink reader = engine.add(new Sink("V", Families.HC_IN));
        Wire line = new Wire("LINE", 10e-12);
        src.vdd.connect(rail);
        src.gnd.connect(localGround);
        src.out.connect(line);
        load.a().connect(rail);
        load.b().connect(line);
        reader.vdd.connect(rail);
        reader.gnd.connect(ground);
        reader.in.connect(line);
        engine.runUntil(100_000);

        assertTrue(localGround.volts() > 0.5, "la massa locale si alza: " + localGround.volts());
        assertTrue(line.volts() > 1.5, "il livello basso non è più basso: " + line.volts());
        assertEquals(Level.X, reader.in.level(), "e chi legge con la massa vera non lo riconosce");
    }
}
