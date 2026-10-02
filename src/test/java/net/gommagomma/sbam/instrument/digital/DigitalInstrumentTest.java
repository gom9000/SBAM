package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.logic.LogicFunction;
import net.gommagomma.sbam.digital.level.Drive;
import net.gommagomma.sbam.digital.stage.Families;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Sink;
import net.gommagomma.sbam.fixtures.DigitalFixtures.Source;
import net.gommagomma.sbam.fixtures.Ground;
import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Event;
import net.gommagomma.sbam.instrument.EventLog;
import net.gommagomma.sbam.instrument.Recorder;
import net.gommagomma.sbam.instrument.Severity;
import net.gommagomma.sbam.parts.logic.Gate;
import net.gommagomma.sbam.parts.passive.Resistor;
import net.gommagomma.sbam.parts.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Strumenti dello strato digitale: sonda logica e sentinelle. */
class DigitalInstrumentTest
{
    private static Wire rail(Engine engine)
    {
        Supply vcc = engine.add(new Supply("VCC", 5.0, 0.1));
        Wire rail = new Wire("+5V", 100e-12);
        vcc.out().connect(rail);
        return rail;
    }

    @Test
    void aChangeTraceKeepsOnlyTheChanges()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        Source src = engine.add(new Source("SRC", Families.HC_TRISTATE)
                .at(0, Drive.L).at(100_000, Drive.H).at(300_000, Drive.L));
        Sink in = engine.add(new Sink("IN", Families.HC_IN));
        Wire a = new Wire("A", 10e-12);
        src.vdd.connect(rail);
        src.gnd.connect(Ground.of(engine));
        in.vdd.connect(rail);
        in.gnd.connect(Ground.of(engine));
        src.out.connect(a);
        in.in.connect(a);
        ChangeTrace t = new ChangeTrace();
        engine.add(new Recorder(t).add(new DriveSignal(src.out)).add(new LevelSignal(in.in)));
        engine.runUntil(1_000_000);

        assertEquals(3, t.changes(0), "intenzione: L, H, L");
        assertEquals(5, t.changes(1), "livello letto: L, X, H, X, L (i fronti attraversano la zona indefinita)");
        assertEquals(8, t.size(), "mille tick, otto voci");
        assertEquals('H', t.logicAt(0, 100_000));
        assertEquals('L', t.logicAt(1, 100_000), "l'intenzione arriva prima del livello");
        assertEquals('X', t.logicAt(1, 102_000), "a metà del fronte RC");
        assertEquals('H', t.logicAt(1, 110_000), "il fronte RC sale in pochi ns");
        assertEquals('L', t.logicAt(1, 900_000));
        assertEquals("SRC.Y.drive", t.columns().get(0));
    }

    @Test
    void contentionSentinelReportsTwoOutputsInDisagreement()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        Source up = engine.add(new Source("UP", Families.HC_TRISTATE).at(0, Drive.H));
        Source down = engine.add(new Source("DOWN", Families.HC_TRISTATE)
                .at(0, Drive.Z).at(100_000, Drive.L).at(300_000, Drive.Z));
        Wire bus = new Wire("BUS", 10e-12);
        up.vdd.connect(rail);
        up.gnd.connect(Ground.of(engine));
        down.vdd.connect(rail);
        down.gnd.connect(Ground.of(engine));
        up.out.connect(bus);
        down.out.connect(bus);
        EventLog log = new EventLog();
        engine.add(new ContentionSentinel(log));
        engine.runUntil(1_000_000);

        assertEquals(2, log.count(Severity.KABOOM), "inizio e fine: " + log.events());
        Event start = log.events().get(0), end = log.events().get(1);
        assertEquals(100_000, start.timePs());
        assertTrue(start.message().contains("UP.Y=H") && start.message().contains("DOWN.Y=L"), start.message());
        assertEquals(300_000, end.timePs());
        assertTrue(end.message().contains("200 ns"), end.message());
    }

    @Test
    void outputsThatAgreeOrReleaseAreNotAContention()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        Source a = engine.add(new Source("A", Families.HC_TRISTATE).at(0, Drive.H));
        Source b = engine.add(new Source("B", Families.HC_TRISTATE).at(0, Drive.H).at(200_000, Drive.Z));
        Wire bus = new Wire("BUS", 10e-12);
        a.vdd.connect(rail);
        a.gnd.connect(Ground.of(engine));
        b.vdd.connect(rail);
        b.gnd.connect(Ground.of(engine));
        a.out.connect(bus);
        b.out.connect(bus);
        EventLog log = new EventLog();
        engine.add(new ContentionSentinel(log));
        engine.runUntil(500_000);
        assertTrue(log.events().isEmpty(), log.events().toString());
    }

    /** Un filo a una tensione fissa, attraverso una resistenza. */
    private static Wire held(Engine engine, String name, double volts, double ohms)
    {
        Supply s = engine.add(new Supply("V_" + name, volts, 0.1));
        Resistor r = engine.add(new Resistor("R_" + name, ohms));
        Wire src = new Wire(name + "_SRC", 10e-12);
        Wire w = new Wire(name, 10e-12);
        s.out().connect(src);
        r.a().connect(src);
        r.b().connect(w);
        return w;
    }

    @Test
    void ratingSentinelUsesTheLimitOfEachStage()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        Source heavy = engine.add(new Source("HEAVY", Families.HC_TRISTATE).at(0, Drive.H).at(300_000, Drive.Z));
        Source light = engine.add(new Source("LIGHT", Families.HC_TRISTATE).at(0, Drive.H));
        heavy.vdd.connect(rail);
        heavy.gnd.connect(Ground.of(engine));
        light.vdd.connect(rail);
        light.gnd.connect(Ground.of(engine));
        heavy.out.connect(held(engine, "LOAD100", 0.0, 100));        // ~24 mA: oltre i 6 mA dell'HC
        light.out.connect(held(engine, "LOAD10K", 0.0, 10_000));     // ~0,5 mA
        EventLog log = new EventLog();
        engine.runUntil(10_000);                                     // dopo l'accensione
        engine.add(new RatingSentinel(log, 20_000));
        engine.runUntil(1_000_000);

        assertEquals(2, log.count(Severity.BZZT), log.events().toString());
        assertEquals("HEAVY.Y", log.events().get(0).source());
        assertTrue(log.events().get(0).message().contains("6,00 mA"), log.events().get(0).message());
        assertEquals(300_000, log.events().get(1).timePs(), "rientra appena l'uscita rilascia");
    }

    @Test
    void floatingSentinelReportsALineNobodyDrives()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        Source src = engine.add(new Source("SRC", Families.HC_TRISTATE)
                .at(0, Drive.H).at(200_000, Drive.Z).at(500_000, Drive.L));
        Sink in = engine.add(new Sink("IN", Families.HC_IN));
        Wire bus = new Wire("BUS", 10e-12);
        src.vdd.connect(rail);
        src.gnd.connect(Ground.of(engine));
        in.vdd.connect(rail);
        in.gnd.connect(Ground.of(engine));
        src.out.connect(bus);
        in.in.connect(bus);
        EventLog log = new EventLog();
        engine.runUntil(10_000);
        engine.add(new FloatingSentinel(log));
        engine.runUntil(1_000_000);

        assertEquals(2, log.events().size(), log.events().toString());
        Event start = log.events().get(0), end = log.events().get(1);
        assertEquals("BUS", start.source());
        assertTrue(start.timePs() >= 200_000 && start.timePs() <= 202_000, "rilasciato a 200 ns: " + start);
        assertTrue(start.message().contains("IN.A=H"), "legge ancora H dalla carica rimasta: " + start.message());
        assertTrue(end.timePs() >= 500_000 && end.timePs() <= 502_000, end.toString());
    }

    @Test
    void undefinedSentinelIgnoresEdgesButNotAStuckMidLevel()
    {
        Engine engine = new Engine(1_000);
        Wire rail = rail(engine);
        Source src = engine.add(new Source("SRC", Families.HC_TRISTATE)
                .at(0, Drive.L).at(100_000, Drive.H).at(200_000, Drive.L));
        Sink edges = engine.add(new Sink("EDGES", Families.HC_IN));
        Sink stuck = engine.add(new Sink("STUCK", Families.HC_IN));
        Gate schmitt = engine.add(new Gate("SCHMITT", Families.HC_SCHMITT_GATE, LogicFunction.NOT, 1, 13_000));   // 74HC14
        Wire a = new Wire("A", 10e-12);
        Wire mid = held(engine, "MID", 2.5, 1_000);                  // a metà: X per un HC normale
        src.vdd.connect(rail);
        src.gnd.connect(Ground.of(engine));
        edges.vdd.connect(rail);
        edges.gnd.connect(Ground.of(engine));
        stuck.vdd.connect(rail);
        stuck.gnd.connect(Ground.of(engine));
        schmitt.vdd().connect(rail);
        schmitt.gnd().connect(Ground.of(engine));
        src.out.connect(a);
        edges.in.connect(a);
        stuck.in.connect(mid);
        schmitt.in(0).connect(mid);                                    // l'isteresi non legge mai X
        EventLog log = new EventLog();
        engine.runUntil(10_000);
        engine.add(new UndefinedSentinel(log, 20_000));
        engine.runUntil(1_000_000);

        assertEquals(1, log.events().size(), "solo STUCK, una volta: " + log.events());
        assertEquals("STUCK.A", log.events().get(0).source());
        assertEquals(Severity.CRASH, log.events().get(0).severity());
    }
}
