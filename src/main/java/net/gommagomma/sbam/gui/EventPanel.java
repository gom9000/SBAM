package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.Event;
import net.gommagomma.sbam.instrument.Severity;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Il registro degli eventi in una tabella: quando, quanto è grave, dove, che cosa. La gravità ha il suo colore.
 * Scegliere un evento porta il cursore al suo istante, al centro della vista.
 */
@SuppressWarnings("serial")                 // i componenti Swing qui non si serializzano mai
final class EventPanel extends JScrollPane
{
    private final Runner runner;
    private final TimeView view;
    private final List<Event> events = new ArrayList<>();
    private final Model model = new Model();
    private final JTable table = new JTable(model);

    EventPanel(Runner runner, TimeView view)
    {
        this.runner = runner;
        this.view = view;
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        table.setRowHeight(20);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new Renderer());
        table.getSelectionModel().addListSelectionListener(new Selection());
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(700);
        setViewportView(table);
    }

    /** Aggiunge gli eventi nuovi. */
    void refresh()
    {
        int known = events.size();
        synchronized (runner.lock()) {
            List<Event> all = runner.simulation().log().events();
            events.addAll(all.subList(known, all.size()));
        }
        if (events.size() == known) return;
        model.fireTableRowsInserted(known, events.size() - 1);
        if (table.getSelectedRow() < 0) table.scrollRectToVisible(table.getCellRect(events.size() - 1, 0, true));
    }

    /** Quanti eventi ci sono. */
    int count()     { return events.size(); }

    static Color color(Severity s)
    {
        switch (s) {
            case DING:   return new Color(0x30, 0x60, 0xb0);
            case BZZT:   return new Color(0x9a, 0x6a, 0x00);
            case ZAP:    return new Color(0xb0, 0x50, 0x00);
            case CRASH:  return new Color(0xb0, 0x20, 0x20);
            case BOING:  return new Color(0x90, 0x20, 0xa0);
            case KABOOM: return new Color(0xd0, 0x00, 0x00);
            default: throw new IllegalArgumentException("gravità " + s);
        }
    }

    private final class Model extends AbstractTableModel
    {
        @Override public int getRowCount()     { return events.size(); }
        @Override public int getColumnCount()  { return 4; }

        @Override
        public String getColumnName(int column)
        {
            switch (column) {
                case 0: return "quando";
                case 1: return "gravità";
                case 2: return "dove";
                case 3: return "che cosa";
                default: throw new IllegalArgumentException("colonna " + column);
            }
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            Event e = events.get(row);
            switch (column) {
                case 0: return Quantities.time(e.timePs());
                case 1: return e.severity().sound();
                case 2: return e.source();
                case 3: return e.message();
                default: throw new IllegalArgumentException("colonna " + column);
            }
        }
    }

    private final class Renderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (!selected) c.setForeground(column == 3 ? Color.DARK_GRAY : color(events.get(row).severity()));
            c.setFont(column == 1 ? t.getFont().deriveFont(Font.BOLD) : t.getFont());
            return c;
        }
    }

    private final class Selection implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            long t = events.get(row).timePs();
            view.cursor(t);
            view.center(t);
        }
    }
}
