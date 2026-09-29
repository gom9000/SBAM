package net.gommagomma.sbam.demo;


import net.gommagomma.sbam.engine.Engine;
import net.gommagomma.sbam.parts.Specs;
import net.gommagomma.sbam.physics.Level;
import net.gommagomma.sbam.physics.Line;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Rail;

import java.util.Locale;

import static net.gommagomma.sbam.engine.Engine.ns;


/**
 * Prova della fisica degli ingressi.
 *
 * 1) Isteresi: la stessa linea /IRQ con pull-up da 10k, letta da un ingresso TTL del PIC,
 *    da un 74HC, da un 74HC14 (Schmitt) e da un ingresso Schmitt del PIC.
 *
 * 2) Diodi di protezione: un 74HC244 alimentato alimenta a sua volta, attraverso un ingresso,
 *    un chip la cui alimentazione è spenta.
 */
public class InputDemo
{
    public static void main(String[] args)
    {
        System.out.println("=== 1. Fronte lento letto da ingressi diversi ===");
        hysteresis();

        System.out.println();
        System.out.println("=== 2. Un chip spento alimentato dai suoi ingressi ===");
        phantomPower();
    }

    private static void hysteresis()
    {
        Engine engine = new Engine(ns(1));
        Rail vcc = engine.add(Rail.ideal("+5V", 5.0));

        Line irq = engine.add(new Line("/IRQ").wire(10).pullUp(vcc, 10_000));
        Pin ra4 = Pin.output("PIC648.RA4", vcc, Specs.PIC16_RA4);
        Pin ttl = Pin.input("PIC877.RB0 (TTL)", vcc, Specs.PIC16_IN);
        Pin hc = Pin.input("74HC", vcc, Specs.HC_IN);
        Pin hc14 = Pin.input("74HC14", vcc, Specs.HC14_IN);
        Pin st = Pin.input("PIC877.RD0 (ST)", vcc, Specs.PIC16_ST_IN);
        irq.connect(ra4).connect(ttl).connect(hc).connect(hc14).connect(st);

        engine.at(ns(100), () -> ra4.drive(Level.L));
        engine.at(ns(300), ra4::release);

        LineDemo.watch(engine, irq, ttl, hc, hc14, st);
        engine.runUntil(ns(1000));
    }

    private static void phantomPower()
    {
        Engine engine = new Engine(ns(10));
        Rail on = engine.add(Rail.ideal("+5V", 5.0));
        // alimentazione spenta: regolatore che non conduce (100 kohm di perdita) e 1 uF di bypass
        Rail off = engine.add(new Rail("+5V_B", 0.0, 100_000, 1.0));

        Line a = engine.add(new Line("A0").wire(10));
        Pin out = Pin.output("U1 (74HC244, acceso)", on, Specs.HC244_OUT);
        Pin in = Pin.input("U9 (74HC, spento)", off, Specs.HC_IN);
        a.connect(out).connect(in);

        engine.at(0, () -> out.drive(Level.H));
        for (int us = 0; us <= 200; us += 25) {
            engine.runUntil(ns(us * 1000.0));
            System.out.printf(Locale.ITALIAN, "  %4d us: linea %.2f V, rail spenta %.2f V, corrente nel diodo %.1f mA%n",
                    us, a.getVolts(), off.getVolts(), -off.getLoadMA());
        }
    }
}
