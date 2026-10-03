package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.hardware.cpu.Cpu;
import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.program.Disassembler;
import net.gommagomma.sbam.program.Instruction;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Il pannello di una CPU: i registri (PC, A, SP), l'istruzione in corso con il ciclo macchina e la fase, lo stato
 * (in corsa, ferma, guasta), e le istruzioni eseguite, le ultime in fondo. Scegliere un'istruzione eseguita porta
 * il cursore al suo istante. Legge la CPU tenendo il lock del runner.
 */
@SuppressWarnings("serial")                 // i componenti Swing qui non si serializzano mai
final class CpuPanel extends JPanel
{
    private final Runner runner;
    private final CpuWatch watch;
    private final TimeView view;
    private final JLabel pc = value(), a = value(), sp = value(), now = value(), state = value();
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private int rows = 0;
    private long[] times = new long[0];
    private int[] addresses = new int[0];
    private String[] texts = new String[0];

    CpuPanel(Runner runner, CpuWatch watch, TimeView view)
    {
        super(new BorderLayout());
        this.runner = runner;
        this.watch = watch;
        this.view = view;
        JPanel registers = new JPanel(new GridLayout(0, 2, 8, 2));
        registers.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        row(registers, "PC", pc);
        row(registers, "A", a);
        row(registers, "SP", sp);
        row(registers, "in corso", now);
        row(registers, "stato", state);
        add(registers, BorderLayout.NORTH);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setRowHeight(18);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new Selection());
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /** Rilegge la CPU e le istruzioni eseguite. */
    void refresh()
    {
        int n;
        synchronized (runner.lock()) {
            Cpu cpu = watch.cpu();
            pc.setText(String.format("%04X", cpu.pc()));
            a.setText(String.format("%02X   (%d)", cpu.accumulator(), cpu.accumulator()));
            sp.setText(String.format("%04X", cpu.stack()));
            Instruction i = cpu.instruction();
            now.setText(cpu.halted() ? "-" : i == null ? "lettura del codice"
                    : Disassembler.text(i, cpu.operand()) + "   ciclo " + (cpu.machineCycle() + 1) + "/" + cpu.cycles(i)
                    + ", Q" + (cpu.quarter() + 1));
            state.setText(cpu.fault() != null ? "GUASTO: " + cpu.fault() : cpu.halted() ? "ferma (HLT)" : "in esecuzione");
            n = watch.size();
            if (n != rows || (n > 0 && times.length > 0 && times[n - 1] != watch.time(n - 1))) {
                times = new long[n];
                addresses = new int[n];
                texts = new String[n];
                for (int k = 0; k < n; k++) {
                    times[k] = watch.time(k);
                    addresses[k] = watch.address(k);
                    texts[k] = watch.text(k);
                }
            } else {
                return;
            }
        }
        rows = n;
        model.fireTableDataChanged();
        if (table.getSelectedRow() < 0 && n > 0) table.scrollRectToVisible(table.getCellRect(n - 1, 0, true));
    }

    String label()      { return watch.label(); }

    private static JLabel value()
    {
        JLabel l = new JLabel("-");
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        return l;
    }

    private static void row(JPanel panel, String name, JLabel value)
    {
        JLabel l = new JLabel(name);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        panel.add(l);
        panel.add(value);
    }

    private final class Model extends AbstractTableModel
    {
        @Override public int getRowCount()     { return rows; }
        @Override public int getColumnCount()  { return 3; }

        @Override
        public String getColumnName(int column)
        {
            switch (column) {
                case 0: return "fine";
                case 1: return "indirizzo";
                case 2: return "istruzione";
                default: throw new IllegalArgumentException("colonna " + column);
            }
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            switch (column) {
                case 0: return Quantities.time(times[row]);
                case 1: return String.format("%04X", addresses[row]);
                case 2: return texts[row];
                default: throw new IllegalArgumentException("colonna " + column);
            }
        }
    }

    private final class Selection implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0 || row >= rows) return;
            view.cursor(times[row]);
            view.center(times[row]);
        }
    }
}
