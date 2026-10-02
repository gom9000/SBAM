package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.instrument.ChangeTrace;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

/**
 * L'analizzatore logico: le sonde logiche e le parole.
 *
 *  - H e L: una linea in alto o in basso;
 *  - Z (nessuno pilota): una linea a metà, gialla;
 *  - X (valore non determinato): una fascia rossa;
 *  - una parola: una fascia con il valore in esadecimale dentro, se ci sta;
 *  - dove i cambiamenti sono più fitti di un paio di pixel: una fascia tratteggiata, "qui c'è attività";
 *    ingrandendo si vedono i singoli fronti.
 */
@SuppressWarnings("serial")                 // i componenti Swing qui non si serializzano mai
final class LogicPanel extends TimePanel
{
    private static final Color LEVEL = new Color(0x5c, 0xd0, 0x6a);
    private static final Color UNDEFINED = new Color(0xe0, 0x4a, 0x4a);
    private static final Color RELEASED = new Color(0xe0, 0xb8, 0x3a);
    private static final Color WORD = new Color(0x5a, 0xc8, 0xd8);
    private static final Color WORD_FILL = new Color(0x1f, 0x3a, 0x40);

    /** Sotto questa larghezza un tratto non si disegna da solo: fa parte di una zona fitta. */
    private static final int DENSE = 3;

    LogicPanel(Runner runner, ChangeTrace trace, TimeView view, List<Integer> columns, List<String> labels)
    {
        super(runner, trace, view, columns, labels);
    }

    @Override
    protected int height(int column)
    {
        return trace.isWord(column) ? 36 : 30;
    }

    @Override
    protected void track(Graphics2D g, int column, int h, long nowPs)
    {
        long t0 = view.startPs();
        long right = Math.min(view.endPs(), nowPs);
        if (right < t0) return;
        boolean word = trace.isWord(column);
        int top = 7, bottom = h - 7;
        int xr = x(right);
        int denseRight = Integer.MIN_VALUE;           // il bordo destro della zona fitta in corso
        int i = at(column, right);
        while (i >= 0) {
            long t = trace.time(i);
            int xl = x(Math.max(t, t0));
            if (t > t0 && xr - xl < DENSE) {              // troppo stretto: fa parte di una zona fitta
                if (denseRight == Integer.MIN_VALUE) denseRight = xr;
                xr = xl;
                i = trace.previous(i);
                continue;
            }
            if (denseRight != Integer.MIN_VALUE) {
                dense(g, word, xr, denseRight, top, bottom);
                denseRight = Integer.MIN_VALUE;
            }
            if (word) wordSegment(g, column, i, xl, xr, top, bottom, t > t0);
            else logicSegment(g, trace.logic(i), xl, xr, top, bottom);
            if (t <= t0) break;
            int before = trace.previous(i);
            if (!word && before >= 0) edge(g, trace.logic(before), trace.logic(i), xl, top, bottom);
            xr = xl;
            i = before;
        }
        if (denseRight != Integer.MIN_VALUE) dense(g, word, Math.max(xr, 0), denseRight, top, bottom);
    }

    /** Una zona di cambiamenti troppo fitti per vederli uno per uno. */
    private void dense(Graphics2D g, boolean word, int xl, int xr, int top, int bottom)
    {
        Color c = word ? WORD : LEVEL;
        g.setColor(word ? WORD_FILL : new Color(0x24, 0x40, 0x2a));
        g.fillRect(xl, top, Math.max(1, xr - xl), bottom - top + 1);
        g.setColor(c);
        g.drawLine(xl, top, xr, top);
        g.drawLine(xl, bottom, xr, bottom);
        for (int x = xl - (xl % 6); x < xr; x += 6) {
            if (x >= xl) g.drawLine(x, bottom, Math.min(x + 6, xr), top);
        }
    }

    private void logicSegment(Graphics2D g, char level, int xl, int xr, int top, int bottom)
    {
        int middle = (top + bottom) / 2;
        switch (level) {
            case 'H':
                g.setColor(LEVEL);
                g.fillRect(xl, top, xr - xl + 1, 2);
                break;
            case 'L':
                g.setColor(LEVEL);
                g.fillRect(xl, bottom - 1, xr - xl + 1, 2);
                break;
            case 'Z':
                g.setColor(RELEASED);
                g.fillRect(xl, middle, xr - xl + 1, 2);
                break;
            case 'X':
                g.setColor(UNDEFINED);
                if (xr > xl) g.fillRect(xl, top, xr - xl, bottom - top + 1);
                break;
            default:
                throw new IllegalStateException("valore logico non previsto: " + level);
        }
    }

    /** Il tratto verticale tra due livelli. */
    private void edge(Graphics2D g, char from, char to, int x, int top, int bottom)
    {
        int a = levelY(from, top, bottom), b = levelY(to, top, bottom);
        g.setColor(LEVEL);
        g.fillRect(x, Math.min(a, b), 2, Math.abs(a - b) + 1);
    }

    private static int levelY(char level, int top, int bottom)
    {
        switch (level) {
            case 'H': return top;
            case 'L': return bottom;
            case 'Z': return (top + bottom) / 2;
            case 'X': return (top + bottom) / 2;
            default: throw new IllegalStateException("valore logico non previsto: " + level);
        }
    }

    private void wordSegment(Graphics2D g, int column, int change, int xl, int xr, int top, int bottom, boolean starts)
    {
        int middle = (top + bottom) / 2;
        long all = trace.width(column) >= 64 ? -1L : (1L << trace.width(column)) - 1;
        long released = trace.released(change);
        if ((released & all) == all) {
            g.setColor(RELEASED);
            g.fillRect(xl, middle, xr - xl + 1, 2);
            return;
        }
        boolean undefined = (trace.unknown(change) | released) != 0;
        int bevel = starts ? Math.min(4, Math.max(0, (xr - xl) / 2)) : 0;
        int[] px = { xl, xl + bevel, xr, xr, xl + bevel };
        int[] py = { middle, top, top, bottom, bottom };
        g.setColor(undefined ? new Color(0x4a, 0x22, 0x22) : WORD_FILL);
        g.fillPolygon(px, py, 5);
        g.setColor(undefined ? UNDEFINED : WORD);
        g.drawPolyline(px, py, 5);
        String text = Format.value(trace, column, change);
        g.setFont(MONO);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int left = Math.max(xl + bevel, 0), width = xr - left;
        g.setColor(TEXT);
        if (width > tw + 8) g.drawString(text, left + (width - tw) / 2, middle + fm.getAscent() / 2 - 1);
    }
}
