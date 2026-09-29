package net.gommagomma.sbam.demo;


import net.gommagomma.sbam.engine.Engine;
import net.gommagomma.sbam.engine.Log;
import net.gommagomma.sbam.parts.Specs;
import net.gommagomma.sbam.physics.Level;
import net.gommagomma.sbam.physics.Line;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Rail;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static net.gommagomma.sbam.engine.Engine.ns;


/**
 * Prova di linee con il tempo: engine a passi fissi da 1 ns, ancora senza device.
 * Le azioni sono programmate a mano sulla bacheca dell'engine.
 * Alimentazione ideale: qui interessano le linee (per le rail vedi RailDemo).
 */
public class LineDemo
{
    public static void main(String[] args)
    {
        Engine engine = new Engine(ns(1));
        Rail vcc = engine.add(Rail.ideal("+5V", 5.0));

        // ------------------------------------------------ /IRQ open drain con pull-up da 10k
        Line irq = engine.add(new Line("/IRQ").wire(10).pullUp(vcc, 10_000));
        Pin ra4 = new Pin("PIC648.RA4", vcc, Specs.PIC16_RA4, Specs.PIC16_IN);
        Pin rb0 = Pin.input("PIC877.RB0", vcc, Specs.PIC16_IN);
        Pin clr = Pin.input("U5./CLR (74HC)", vcc, Specs.HC_IN);
        irq.connect(ra4).connect(rb0).connect(clr);

        // ------------------------------------------------ D0: buffer 74HC244 e porta del PIC
        Line d0 = engine.add(new Line("D0").wire(6));
        Pin buf = Pin.output("U3 (74HC244)", vcc, Specs.HC244_OUT);
        Pin rd0 = new Pin("PIC877.RD0", vcc, Specs.PIC16_OUT, Specs.PIC16_IN);
        d0.connect(buf).connect(rd0);

        // ------------------------------------------------ cosa succede, e quando
        engine.at(0,        () -> buf.drive(Level.H));
        engine.at(ns(100),  () -> ra4.drive(Level.L));     // il 648 chiede un interrupt
        engine.at(ns(300),  ra4::release);                  // ... e lo rilascia: ora sale il pull-up
        engine.at(ns(1000), () -> rd0.drive(Level.L));     // il PIC pilota D0 mentre lo fa anche il buffer
        engine.at(ns(1030), rd0::release);
        engine.at(ns(1100), buf::release);                  // nessuno pilota più D0

        System.out.printf(Locale.ITALIAN, "/IRQ: %.1f pF, tau con il solo pull-up = %s%n",
                irq.getCapacitancePF(), Log.formatTime(Math.round(10_000 * irq.getCapacitancePF())));
        System.out.printf(Locale.ITALIAN, "D0:   %.1f pF%n%n", d0.getCapacitancePF());

        watch(engine, irq, rb0, clr);
        watch(engine, d0, rd0);

        engine.runUntil(ns(1300));
    }

    /** Stampa tensione e letture ogni volta che uno dei pin osservati cambia livello letto. */
    static void watch(Engine engine, Line line, Pin... pins)
    {
        Map<Pin, Level> last = new HashMap<>();
        engine.probe(() -> {
            boolean changed = false;
            for (Pin p : pins) {
                if (last.get(p) != p.getSeen()) changed = true;
                last.put(p, p.getSeen());
            }
            if (!changed) return;
            StringBuilder sb = new StringBuilder();
            for (Pin p : pins) sb.append("  ").append(p.getName()).append("=").append(p.getSeen());
            System.out.printf(Locale.ITALIAN, "[%10s] %-6s %5.2f V%s%n",
                    Log.formatTime(engine.getNowPs()), line.getName(), line.getVolts(), sb);
        });
    }
}
