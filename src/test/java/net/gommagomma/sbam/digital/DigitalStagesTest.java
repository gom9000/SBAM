package net.gommagomma.sbam.digital;

import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.level.Edge;
import net.gommagomma.sbam.digital.level.Level;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Sink;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.fixtures.Recorders.Edges;
import net.gommagomma.sbam.parts.passive.Resistor;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gli stadi digitali e le famiglie, sui casi classici del banco. */
class DigitalStagesTest
{
    private static Wire rail(Engine engine, String name, double volts)
    {
        Supply s = engine.add(new Supply("PSU " + name, volts, 0.1));
        Wire w = new Wire(name, 100e-12);
        s.out().connect(w);
        return w;
    }

    @Test
    void lsHighIsUndefinedForHcButHighForHct()
    {
        Engine engine = new Engine(1_000);
        Wire v5 = rail(engine, "+5V", 5.0);
        Wire line = new Wire("A0", 10e-12);
        Source ls = engine.add(new Source("U7", Families.LS_TRISTATE).at(0, Drive.H));
        Sink hc = engine.add(new Sink("U2", Families.HC_IN));
        Sink hct = engine.add(new Sink("U3", Families.HCT_IN));
        for (var d : List.of(ls.vdd, hc.vdd, hct.vdd)) d.connect(v5);
        ls.out.connect(line);
        hc.in.connect(line);
        hct.in.connect(line);

        engine.runUntil(1_000_000);
        assertEquals(3.4, line.volts(), 0.05, "un 74LS alto si ferma intorno a 3,4 V");
        assertEquals(Level.X, hc.in.level(), "per un 74HC (VIH 3,5 V) non è un livello valido");
        assertEquals(Level.H, hct.in.level(), "per un 74HCT (VIH 2,0 V) è H");
    }

    @Test
    void threeVoltHcIsUndefinedForFiveVoltHcButHighForTtlInputs()
    {
        Engine engine = new Engine(1_000);
        Wire v5 = rail(engine, "+5V", 5.0);
        Wire v33 = rail(engine, "+3V3", 3.3);
        Wire line = new Wire("A0", 10e-12);
        Source src = engine.add(new Source("U1", Families.HC_TRISTATE).at(0, Drive.H));
        Sink hc = engine.add(new Sink("U2", Families.HC_IN));
        Sink pic = engine.add(new Sink("PIC", Families.PIC16_TTL_IN));
        src.vdd.connect(v33);
        src.gnd.connect(Ground.of(engine));
        hc.vdd.connect(v5);
        hc.gnd.connect(Ground.of(engine));
        pic.vdd.connect(v5);
        pic.gnd.connect(Ground.of(engine));
        src.out.connect(line);
        hc.in.connect(line);
        pic.in.connect(line);

        engine.runUntil(1_000_000);
        assertEquals(3.3, line.volts(), 0.01);
        assertEquals(Level.X, hc.in.level());
        assertEquals(Level.H, pic.in.level());
    }

    @Test
    void fiveVoltOutputIntoThreeVoltInputConductsIntoItsRail()
    {
        Engine engine = new Engine(1_000);
        Wire v5 = rail(engine, "+5V", 5.0);
        Supply psu33 = engine.add(new Supply("PSU +3V3", 3.3, 0.1));
        Wire v33 = new Wire("+3V3", 100e-12);
        psu33.out().connect(v33);
        Wire line = new Wire("A0", 10e-12);
        Source src = engine.add(new Source("U1", Families.HC_TRISTATE).at(0, Drive.H));
        Sink low = engine.add(new Sink("U9", Families.HC_IN));
        src.vdd.connect(v5);
        src.gnd.connect(Ground.of(engine));
        low.vdd.connect(v33);
        low.gnd.connect(Ground.of(engine));
        src.out.connect(line);
        low.in.connect(line);

        engine.runUntil(1_000_000);
        // l'uscita (5 V, ~110 ohm) contro il diodo (3,3 + 0,6 V, 20 ohm): partitore tra i due generatori
        double rh = (4.5 - 3.84) / 6e-3, rd = 20.0, vd = 3.3 + 0.6;          // i diodi CMOS delle famiglie
        double expected = (5.0 / rh + vd / rd) / (1 / rh + 1 / rd);
        assertEquals(expected, line.volts(), 0.01, "il diodo tiene la linea poco sopra 3,3 + 0,6 V");
        assertTrue(psu33.out().current() < -5e-3, "l'alimentatore a 3,3 V assorbe la corrente del diodo: "
                + psu33.out().current());
    }

    @Test
    void openDrainNeedsAPullUp()
    {
        Engine engine = new Engine(1_000);
        Wire v5 = rail(engine, "+5V", 5.0);
        Wire irq = new Wire("/IRQ", 15e-12);
        Source ra4 = engine.add(new Source("PIC648", Families.PIC16_RA4_OUT)
                .at(0, Drive.Z).at(1_000_000, Drive.L).at(2_000_000, Drive.Z));
        Sink rb0 = engine.add(new Sink("PIC877", Families.PIC16_ST_IN));
        Resistor pull = engine.add(new Resistor("R10k", 10_000));
        ra4.vdd.connect(v5);
        ra4.gnd.connect(Ground.of(engine));
        rb0.vdd.connect(v5);
        rb0.gnd.connect(Ground.of(engine));
        ra4.out.connect(irq);
        rb0.in.connect(irq);
        pull.a().connect(v5);
        pull.b().connect(irq);

        engine.runUntil(900_000);
        assertEquals(Level.H, rb0.in.level(), "rilasciato: il pull-up tiene alto");
        engine.runUntil(1_500_000);
        assertEquals(Level.L, rb0.in.level());
        engine.runUntil(5_000_000);
        assertEquals(Level.H, rb0.in.level());
    }

    @Test
    void stagesRejectWhatTheyCannotDo()
    {
        Source od = new Source("OD", Families.PIC16_RA4_OUT);
        assertThrows(IllegalArgumentException.class, () -> od.out.drive(Drive.H), "un open-drain non pilota H");
        Source tp = new Source("TP", Families.HC_TOTEM);
        assertEquals(Drive.L, tp.out.driven(), "un totem-pole parte pilotando");
        assertThrows(IllegalArgumentException.class, () -> tp.out.drive(Drive.Z), "un totem-pole non rilascia");
    }

    @Test
    void lsInputsLoadALowOutput()
    {
        Engine engine = new Engine(1_000);
        Wire v5 = rail(engine, "+5V", 5.0);
        Wire line = new Wire("CLK", 20e-12);
        Source drv = engine.add(new Source("U3", Families.HC_TRISTATE).at(0, Drive.L));
        drv.vdd.connect(v5);
        drv.gnd.connect(Ground.of(engine));
        drv.out.connect(line);
        List<Sink> loads = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Sink s = engine.add(new Sink("LS" + i, Families.LS_IN));
            s.vdd.connect(v5);
            s.gnd.connect(Ground.of(engine));
            s.in.connect(line);
            loads.add(s);
        }
        engine.runUntil(1_000_000);

        double expected = 20 * 0.4e-3 * (0.33 / 6e-3);   // 20 ingressi da 0,4 mA su 55 ohm
        assertEquals(expected, line.volts(), 0.01, "il livello basso si alza con il fan-out");
        assertEquals(-8e-3, drv.out.current(), 2e-4, "l'uscita assorbe 8 mA, oltre i 6 garantiti");
        for (Sink s : loads) assertEquals(Level.L, s.in.level());
    }

    @Test
    void edgesAreReportedOncePerChange()
    {
        Engine engine = new Engine(1_000);
        Wire v5 = rail(engine, "+5V", 5.0);
        Wire line = new Wire("D0", 10e-12);
        Source src = engine.add(new Source("U1", Families.HC_TRISTATE)
                .at(0, Drive.L).at(100_000, Drive.H).at(500_000, Drive.L));
        Sink dst = engine.add(new Sink("U2", Families.HC_IN));
        src.vdd.connect(v5);
        src.gnd.connect(Ground.of(engine));
        dst.vdd.connect(v5);
        dst.gnd.connect(Ground.of(engine));
        src.out.connect(line);
        dst.in.connect(line);

        Edges seen = engine.add(new Edges(dst.in));
        engine.runUntil(1_000_000);
        List<Edge> edges = seen.edges();
        List<Long> times = seen.times();

        assertEquals(List.of(Edge.FALLING, Edge.RISING, Edge.FALLING), edges, "prima discesa all'avvio, poi su e giù");
        assertTrue(times.get(1) > 100_000 && times.get(1) < 110_000, "salita poco dopo 100 ns: " + times.get(1));
        assertTrue(times.get(2) > 500_000 && times.get(2) < 510_000, "discesa poco dopo 500 ns: " + times.get(2));
    }

    @Test
    void intentionsAreDecidedOnlyInTheLogic()
    {
        Engine engine = new Engine(1_000);
        Wire v5 = rail(engine, "+5V", 5.0);
        Source src = engine.add(new Source("U1", Families.HC_TRISTATE));
        src.vdd.connect(v5);
        src.gnd.connect(Ground.of(engine));
        src.out.connect(new Wire("D0", 10e-12));
        engine.step();
        assertThrows(IllegalStateException.class, () -> src.out.drive(Drive.H), "fuori dalla logica: rifiutato");
    }
}
