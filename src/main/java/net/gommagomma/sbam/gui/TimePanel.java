package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.ChangeTrace;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;

/**
 * Un pannello di tracce nel tempo: a sinistra il nome di ogni sonda e il suo valore al cursore (o adesso),
 * in alto il righello del tempo, poi una traccia per sonda. Come si disegna una traccia lo decide
 * chi lo estende: l'analizzatore logico, l'oscilloscopio.
 *
 * Mouse: la rotella ingrandisce attorno al puntatore, trascinare sposta, un clic mette il cursore,
 * il tasto destro lo toglie. Legge la traccia tenendo il lock del runner.
 */
@SuppressWarnings("serial")                 // i componenti Swing qui non si serializzano mai
abstract class TimePanel extends JComponent implements Scrollable
{
    static final int NAMES = 210;
    static final int RULER = 24;

    static final Color BACKGROUND = new Color(0x1c, 0x1e, 0x22);
    static final Color NAME_AREA = new Color(0x2a, 0x2d, 0x33);
    static final Color GRID = new Color(0x36, 0x3a, 0x41);
    static final Color TEXT = new Color(0xe6, 0xe8, 0xeb);
    static final Color DIM = new Color(0x95, 0x9b, 0xa3);
    static final Color CURSOR = new Color(0xff, 0xff, 0xff);
    static final Color PRESENT = new Color(0x4a, 0x90, 0xe2);

    static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    static final Font MONO = new Font(Font.MONOSPACED, Font.BOLD, 12);

    private static final Stroke DASHED = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 4f, 3f }, 0f);

    protected final Runner runner;
    protected final ChangeTrace trace;
    protected final TimeView view;
    private final List<Integer> columns;
    private final List<String> labels;

    /** @param labels il nome da mostrare per ogni colonna della traccia */
    TimePanel(Runner runner, ChangeTrace trace, TimeView view, List<Integer> columns, List<String> labels)
    {
        this.runner = runner;
        this.trace = trace;
        this.view = view;
        this.columns = columns;
        this.labels = labels;
        view.attach(this);
        MouseHandler mouse = new MouseHandler();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        setOpaque(true);
    }

    /** L'altezza della traccia di una colonna. */
    protected abstract int height(int column);

    /** Disegna la traccia di una colonna nel suo riquadro (0, 0) - (larghezza, h), fino all'istante attuale. */
    protected abstract void track(Graphics2D g, int column, int h, long nowPs);

    boolean isEmpty()      { return columns.isEmpty(); }

    @Override
    public Dimension getPreferredSize()
    {
        int h = RULER;
        for (int c : columns) h += height(c);
        return new Dimension(900, h + 4);
    }

    // ------------------------------------------------------------ Scrollable: largo quanto la vista, alto quanto le tracce

    @Override public Dimension getPreferredScrollableViewportSize()                  { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction)  { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return r.height; }
    @Override public boolean getScrollableTracksViewportWidth()                      { return true; }
    @Override public boolean getScrollableTracksViewportHeight()                     { return false; }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            view.width(w - NAMES);
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, w, h);
            synchronized (runner.lock()) {
                long now = runner.simulation().engine().nowPs();
                view.present(now);
                ruler(g, w, h);
                long at = view.cursorPs() >= 0 ? view.cursorPs() : now;
                int y = RULER;
                for (int c : columns) {
                    int th = height(c);
                    Graphics2D clip = (Graphics2D) g.create(NAMES, y, w - NAMES, th);
                    try {
                        track(clip, c, th, now);
                    } finally {
                        clip.dispose();
                    }
                    name(g, c, y, th, at);
                    g.setColor(GRID);
                    g.drawLine(0, y + th - 1, w, y + th - 1);
                    y += th;
                }
                marker(g, now, PRESENT, h, false);
                if (view.cursorPs() >= 0) marker(g, view.cursorPs(), CURSOR, h, true);
            }
        } finally {
            g.dispose();
        }
    }

    // ------------------------------------------------------------ per chi estende

    /** Il pixel (nel riquadro della traccia) di un istante. */
    protected int x(long ps)  { return view.x(ps); }

    /** Il cambiamento della colonna in vigore all'istante dato (-1 se non ce n'è). */
    protected int at(int column, long ps)  { return trace.at(column, ps); }

    // ------------------------------------------------------------ interni

    /** Il nome della sonda e il suo valore al cursore (o adesso): sotto il nome se c'è posto, altrimenti accanto. */
    private void name(Graphics2D g, int column, int y, int h, long at)
    {
        g.setColor(NAME_AREA);
        g.fillRect(0, y, NAMES, h);
        g.setColor(GRID);
        g.drawLine(NAMES - 1, y, NAMES - 1, y + h);
        int change = trace.at(column, at);
        String value = change < 0 ? "-" : Format.value(trace, column, change);
        g.setFont(LABEL);
        FontMetrics fm = g.getFontMetrics();
        g.setFont(MONO);
        int vw = g.getFontMetrics().stringWidth(value);
        int line = fm.getAscent() + 3;
        boolean below = h >= 2 * line + 2;
        g.setColor(DIM);
        if (below) g.drawString(value, 8, y + 2 * line);
        else g.drawString(value, NAMES - 10 - vw, y + line);
        g.setFont(LABEL);
        g.setColor(TEXT);
        int room = below ? NAMES - 16 : NAMES - 26 - vw;
        g.drawString(shorten(labels.get(column), fm, room), 8, y + line);
    }

    private void ruler(Graphics2D g, int w, int h)
    {
        g.setColor(NAME_AREA);
        g.fillRect(0, 0, w, RULER);
        long step = Math.max(1, Math.round(niceStep(view.psPerPixel() * 100)));
        long first = (view.startPs() / step) * step;
        g.setFont(SMALL);
        for (long t = first; t <= view.endPs(); t += step) {
            int x = NAMES + x(t);
            if (x < NAMES) continue;
            g.setColor(GRID);
            g.drawLine(x, RULER, x, h);
            g.setColor(DIM);
            g.drawLine(x, RULER - 6, x, RULER);
            g.setColor(TEXT);
            g.drawString(t == 0 ? "0" : Quantities.time(t), x + 4, RULER - 8);
        }
        g.setColor(NAME_AREA);
        g.fillRect(0, 0, NAMES, RULER);
        g.setColor(DIM);
        g.drawString("una tacca = " + Quantities.time(step), 8, RULER - 8);
    }

    private void marker(Graphics2D g, long ps, Color color, int h, boolean dashed)
    {
        int x = NAMES + x(ps);
        if (x < NAMES || x > getWidth()) return;
        Stroke old = g.getStroke();
        if (dashed) g.setStroke(DASHED);
        g.setColor(color);
        g.drawLine(x, RULER, x, h);
        g.setStroke(old);
    }

    /** Un passo di 1, 2 o 5 per una potenza di 10, almeno di quel valore: per il righello e per le scale. */
    static double niceStep(double min)
    {
        double p = Math.pow(10, Math.floor(Math.log10(min)));
        if (p >= min) return p;
        if (2 * p >= min) return 2 * p;
        if (5 * p >= min) return 5 * p;
        return 10 * p;
    }

    private static String shorten(String s, FontMetrics fm, int max)
    {
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Rotella: ingrandisce; trascinare: sposta; clic: cursore (tasto destro: lo toglie). */
    private final class MouseHandler extends MouseAdapter
    {
        private int lastX;
        private boolean dragged;

        @Override
        public void mousePressed(MouseEvent e)
        {
            lastX = e.getX();
            dragged = false;
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            int dx = e.getX() - lastX;
            if (dx != 0) {
                dragged = true;
                view.pan(dx);
                lastX = e.getX();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (dragged || e.getX() < NAMES) return;
            if (e.getButton() == MouseEvent.BUTTON3) view.cursor(-1);
            else view.cursor(view.time(e.getX() - NAMES));
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            if (e.getX() < NAMES) return;
            double factor = e.getWheelRotation() > 0 ? 1.25 : 0.8;
            if (view.following()) view.zoom(factor, getWidth() - NAMES);     // seguendo il presente: resta sul bordo destro
            else view.zoom(factor, e.getX() - NAMES);
        }
    }
}
