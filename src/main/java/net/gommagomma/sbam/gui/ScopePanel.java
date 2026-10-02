package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.instrument.ChangeTrace;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

/**
 * L'oscilloscopio: le sonde analogiche (tensioni, correnti), una traccia per sonda, con una scala verticale
 * a valori tondi (1, 2 o 5 per una potenza di 10) adattata a quello che si vede, e le sue righe orizzontali.
 *
 * La traccia tiene un campione ogni volta che il valore si sposta di almeno una risoluzione: fino al
 * campione dopo, il valore resta quello (entro la risoluzione).
 */
@SuppressWarnings("serial")                 // i componenti Swing qui non si serializzano mai
final class ScopePanel extends TimePanel
{
    private static final Color VOLTS = new Color(0xf0, 0xdc, 0x4a);
    private static final Color AMPS = new Color(0xe0, 0x78, 0xe8);
    private static final Color LABEL_BOX = new Color(0x1c, 0x1e, 0x22, 0xd0);

    ScopePanel(Runner runner, ChangeTrace trace, TimeView view, List<Integer> columns, List<String> labels)
    {
        super(runner, trace, view, columns, labels);
    }

    @Override
    protected int height(int column)
    {
        return 110;
    }

    @Override
    protected void track(Graphics2D g, int column, int h, long nowPs)
    {
        long t0 = view.startPs();
        long right = Math.min(view.endPs(), nowPs);
        int last = at(column, right);
        if (right < t0 || last < 0) return;

        // la scala: dal minimo al massimo di quello che si vede, arrotondati a un passo tondo
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int i = last; i >= 0; i = trace.previous(i)) {
            double v = trace.analog(i);
            min = Math.min(min, v);
            max = Math.max(max, v);
            if (trace.time(i) <= t0) break;
        }
        if (max - min < 1e-12) {
            double pad = Math.max(Math.abs(max) * 0.1, 1e-6);
            min -= pad;
            max += pad;
        }
        double step = niceStep((max - min) / 4);
        double low = Math.floor(min / step) * step, high = Math.ceil(max / step) * step;
        int top = 8, bottom = h - 10;
        double scale = (bottom - top) / (high - low);

        // le righe della scala, con i loro valori
        String unit = trace.unit(column);
        g.setFont(SMALL);
        FontMetrics fm = g.getFontMetrics();
        for (double v = low; v <= high + step / 2; v += step) {
            int y = bottom - (int) Math.round((v - low) * scale);
            g.setColor(Math.abs(v) < step / 2 ? DIM : GRID);
            g.drawLine(0, y, getWidth(), y);
        }

        g.setColor("A".equals(unit) ? AMPS : VOLTS);
        int xr = x(right);
        int yr = bottom - (int) Math.round((trace.analog(last) - low) * scale);
        for (int i = last; i >= 0; i = trace.previous(i)) {
            long t = trace.time(i);
            int yi = bottom - (int) Math.round((trace.analog(i) - low) * scale);
            int xi = x(Math.max(t, t0));
            g.fillRect(xi, yi, Math.max(1, xr - xi + 1), 2);              // fino al campione dopo il valore resta questo
            if (i != last) g.fillRect(xr, Math.min(yi, yr), 2, Math.abs(yr - yi) + 1);   // poi passa al campione dopo
            if (t <= t0) break;
            xr = xi;
            yr = yi;
        }

        for (double v = low; v <= high + step / 2; v += step) {       // i valori sopra la traccia, a sinistra
            int y = bottom - (int) Math.round((v - low) * scale);
            String text = Format.analog(Math.abs(v) < step / 2 ? 0 : v, unit);
            int tw = fm.stringWidth(text);
            g.setColor(LABEL_BOX);
            g.fillRect(2, y - fm.getAscent() / 2 - 2, tw + 6, fm.getAscent() + 3);
            g.setColor(DIM);
            g.drawString(text, 5, y + fm.getAscent() / 2 - 1);
        }
    }
}
