package net.gommagomma.sbam.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * La finestra sul tempo che i pannelli condividono, come gli strumenti sincronizzati di un banco:
 * da dove si guarda, quanti ps per pixel, il cursore, e se seguire il presente.
 */
final class TimeView
{
    private final List<Component> views = new ArrayList<>();
    private long startPs = 0;
    private double psPerPixel;
    private long cursorPs = -1;
    private boolean follow = true;
    private int widthPx = 800;

    TimeView(double psPerPixel)
    {
        this.psPerPixel = psPerPixel;
    }

    /** Un pannello da ridisegnare quando la vista cambia. */
    void attach(Component view)  { views.add(view); }

    long startPs()               { return startPs; }
    long endPs()                 { return startPs + Math.round(widthPx * psPerPixel); }
    double psPerPixel()          { return psPerPixel; }
    long cursorPs()              { return cursorPs; }
    boolean following()          { return follow; }

    /** La larghezza dell'area delle tracce, in pixel (la dicono i pannelli). */
    void width(int px)           { if (px > 0) widthPx = px; }

    /** Il pixel di un istante, dall'inizio dell'area delle tracce. */
    int x(long ps)
    {
        double x = (ps - startPs) / psPerPixel;
        if (x < -10_000) return -10_000;
        if (x > 100_000) return 100_000;
        return (int) Math.round(x);
    }

    /** L'istante di un pixel. */
    long time(int x)             { return startPs + Math.round(x * psPerPixel); }

    /** Ingrandisce (factor < 1) o rimpicciolisce (factor > 1) tenendo fermo l'istante sotto il pixel x. */
    void zoom(double factor, int x)
    {
        long at = time(x);
        psPerPixel = Math.max(1.0, Math.min(1e12, psPerPixel * factor));
        startPs = Math.max(0, at - Math.round(x * psPerPixel));
        changed();
    }

    /** Sposta la vista di dx pixel; smette di seguire il presente. */
    void pan(int dx)
    {
        follow = false;
        startPs = Math.max(0, startPs - Math.round(dx * psPerPixel));
        changed();
    }

    void cursor(long ps)
    {
        cursorPs = ps;
        changed();
    }

    /** Porta un istante al centro della vista; smette di seguire il presente. */
    void center(long ps)
    {
        follow = false;
        startPs = Math.max(0, ps - Math.round(widthPx * psPerPixel / 2));
        changed();
    }

    void follow(boolean on)
    {
        follow = on;
        changed();
    }

    /** Se segue il presente, tiene l'istante attuale sul bordo destro. */
    void present(long nowPs)
    {
        if (!follow) return;
        startPs = Math.max(0, nowPs - Math.round(widthPx * psPerPixel));
    }

    /** Tutto, da 0 al presente. */
    void fit(long nowPs)
    {
        follow = false;
        startPs = 0;
        psPerPixel = Math.max(1.0, nowPs / (double) widthPx);
        changed();
    }

    void changed()
    {
        for (Component c : views) c.repaint();
    }
}
