package net.gommagomma.sbam.demo;


import net.gommagomma.sbam.engine.Engine;
import net.gommagomma.sbam.engine.Log;
import net.gommagomma.sbam.parts.Specs;
import net.gommagomma.sbam.physics.Level;
import net.gommagomma.sbam.physics.Line;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Rail;

import java.util.Locale;

import static net.gommagomma.sbam.engine.Engine.ns;


/**
 * Prova delle alimentazioni.
 *
 * 1) Un bus a 8 bit pilotato da un 74HC244 commuta tutto insieme (0x00 -> 0xFF -> 0x00 -> 0xFF).
 *    Stessa alimentazione (0,5 ohm tra regolatore e chip), con 100 nF di bypass e con solo 1 nF.
 *
 * 2) Due alimentazioni: un'uscita 74HC a 3,3 V che entra in un 74HC a 5 V e in un PIC a 5 V.
 */
public class RailDemo
{
    public static void main(String[] args)
    {
        System.out.println("=== 1. Bus a 8 bit che commuta, con e senza bypass ===");
        busSwitching(0.1);
        busSwitching(0.001);

        System.out.println();
        System.out.println("=== 2. 3,3 V che entrano in chip a 5 V ===");
        mixedRails();
    }

    private static void busSwitching(double bypassUF)
    {
        Engine engine = new Engine(ns(1));
        engine.getLog().setEcho(false);
        Rail vcc = engine.add(new Rail("+5V", 5.0, 0.5, bypassUF).warnDrop(0.02));

        Pin[] out = new Pin[8];
        for (int i = 0; i < 8; i++) {
            Line d = engine.add(new Line("D" + i).wire(40));             // piste lunghe: 40 pF
            out[i] = Pin.output("U3.Y" + i, vcc, Specs.HC244_OUT);
            d.connect(out[i]);
            d.connect(Pin.input("U8.D" + i + " (74HC)", vcc, Specs.HC_IN));
        }

        engine.at(0,       () -> { for (Pin p : out) p.drive(Level.L); });
        engine.at(ns(100), () -> { for (Pin p : out) p.drive(Level.H); });
        engine.at(ns(300), () -> { for (Pin p : out) p.drive(Level.L); });
        engine.at(ns(500), () -> { for (Pin p : out) p.drive(Level.H); });
        engine.runUntil(ns(700));

        System.out.printf(Locale.ITALIAN, "bypass %.0f nF: minimo %.3f V (calo %.0f mV), picco di corrente %.0f mA%n",
                bypassUF * 1000, vcc.getMinVolts(), (vcc.getNominalVolts() - vcc.getMinVolts()) * 1000, vcc.getPeakLoadMA());
        for (Log.Entry e : engine.getLog().getEntries()) System.out.println("    " + e);
    }

    private static void mixedRails()
    {
        Engine engine = new Engine(ns(1));
        Rail v5 = engine.add(Rail.ideal("+5V", 5.0));
        Rail v33 = engine.add(Rail.ideal("+3V3", 3.3));

        Line a = engine.add(new Line("A0").wire(10));
        Pin src = Pin.output("U1 (74HC244 a 3,3 V)", v33, Specs.HC244_OUT);
        Pin hc = Pin.input("U2 (74HC a 5 V)", v5, Specs.HC_IN);
        Pin pic = Pin.input("PIC877 (TTL a 5 V)", v5, Specs.PIC16_IN);
        a.connect(src).connect(hc).connect(pic);

        engine.at(0, () -> src.drive(Level.H));
        engine.runUntil(ns(50));

        System.out.printf(Locale.ITALIAN, "A0 = %.2f V: %s legge %s, %s legge %s%n",
                a.getVolts(), hc.getName(), hc.getSeen(), pic.getName(), pic.getSeen());
    }
}
