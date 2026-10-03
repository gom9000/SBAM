package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.instrument.Quantities;
import net.gommagomma.sbam.instrument.ChangeTrace;
import net.gommagomma.sbam.instrument.Severity;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * La finestra di una simulazione: i comandi del tempo, l'analizzatore logico, l'oscilloscopio e il registro
 * degli eventi.
 *
 *  - Avvia / Pausa: un pulsante solo; Un tick; Reset: ricostruisce il circuito da capo, al tempo 0;
 *  - avanza di un tempo dato, a una velocità scelta; fermati al primo evento abbastanza grave;
 *  - la vista: segui il presente, tutto, ingrandisci, rimpicciolisci; sulle tracce la rotella ingrandisce,
 *    trascinare sposta, un clic mette il cursore (il tasto destro lo toglie).
 *
 *     new SimulationWindow(new MySetup()).show();
 *
 * Il circuito lo costruisce un Setup, e lo ricostruisce a ogni reset. Chiudendo la finestra (o con il reset)
 * la simulazione si chiude e i suoi file vengono scritti.
 */
public final class SimulationWindow
{
    private static final Color RUNNING = new Color(0x1a, 0x7f, 0x37);
    private static final Color STOPPED = new Color(0x9a, 0x30, 0x20);

    private final Setup setup;

    private JFrame frame;
    private JPanel center;
    private JButton playPause;
    private JLabel state, time, cursor;
    private JTextField advance;
    private JComboBox<Speed> speed;
    private JComboBox<StopOn> stopOn;
    private JCheckBox follow;
    private Timer timer;

    // la sessione in corso, e i pannelli che la mostrano
    private Session session;
    private TimeView view;
    private LogicPanel logic;
    private ScopePanel scope;
    private EventPanel events;
    private final List<CpuPanel> cpus = new ArrayList<>();

    public SimulationWindow(Setup setup)
    {
        this.setup = setup;
    }

    /** Apre la finestra (nel thread di Swing); la simulazione resta ferma finché non la si avvia. */
    public void show()
    {
        SwingUtilities.invokeLater(new Build());
    }

    // ------------------------------------------------------------ la finestra

    private void build()
    {
        frame = new JFrame("SBAM");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new Closing());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(toolbar(), BorderLayout.NORTH);
        center = new JPanel(new BorderLayout());
        frame.getContentPane().add(center, BorderLayout.CENTER);
        frame.getContentPane().add(statusBar(), BorderLayout.SOUTH);
        frame.setSize(new Dimension(1280, 860));
        frame.setLocationByPlatform(true);
        startSession();
        timer = new Timer(50, new Refresh());
        timer.start();
        frame.setVisible(true);
    }

    private JToolBar toolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        playPause = button("▶  Avvia", new PlayPause());
        playPause.setPreferredSize(new Dimension(110, playPause.getPreferredSize().height));
        bar.add(playPause);
        bar.add(button("Un tick", new Step()));
        bar.add(button("Reset", new Reset()));
        bar.addSeparator(new Dimension(20, 0));
        bar.add(new JLabel("Avanza di "));
        advance = new JTextField("10us", 6);
        advance.setMaximumSize(advance.getPreferredSize());
        advance.setToolTipText("un tempo con la sua unità: 500ns, 10us, 2ms");
        advance.addActionListener(new Advance());
        bar.add(advance);
        bar.add(button("Vai", new Advance()));
        bar.addSeparator(new Dimension(20, 0));
        bar.add(new JLabel("Velocità "));
        speed = new JComboBox<>(Speed.CHOICES);
        speed.setMaximumSize(speed.getPreferredSize());
        speed.addActionListener(new SpeedChoice());
        bar.add(speed);
        bar.addSeparator(new Dimension(20, 0));
        bar.add(new JLabel("Fermati su "));
        stopOn = new JComboBox<>(StopOn.choices());
        stopOn.setMaximumSize(stopOn.getPreferredSize());
        stopOn.addActionListener(new StopChoice());
        bar.add(stopOn);
        bar.addSeparator(new Dimension(20, 0));
        bar.add(new JLabel("Vista "));
        follow = new JCheckBox("segui il presente", true);
        follow.setFocusable(false);
        follow.addActionListener(new Follow());
        bar.add(follow);
        bar.add(button("Tutto", new Fit()));
        bar.add(button("+", new Zoom(0.5)));
        bar.add(button("−", new Zoom(2.0)));
        return bar;
    }

    private JComponent statusBar()
    {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        state = new JLabel("in pausa");
        state.setFont(state.getFont().deriveFont(Font.BOLD, 14f));
        time = new JLabel(" ");
        time.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        cursor = new JLabel(" ");
        JPanel left = new JPanel(new BorderLayout(16, 0));
        left.add(state, BorderLayout.WEST);
        left.add(time, BorderLayout.CENTER);
        bar.add(left, BorderLayout.WEST);
        bar.add(cursor, BorderLayout.EAST);
        return bar;
    }

    private static JButton button(String text, ActionListener action)
    {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.addActionListener(action);
        return b;
    }

    private static JComponent titled(String title, JComponent content)
    {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        p.add(l, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------ la sessione

    /** Costruisce il circuito da capo (una sessione nuova) e i pannelli che lo mostrano. */
    private void startSession()
    {
        session = new Session(setup);
        Runner runner = session.runner();
        frame.setTitle("SBAM - " + session.simulation().name());
        runner.speed(((Speed) speed.getSelectedItem()).psPerSecond);
        runner.stopOn(((StopOn) stopOn.getSelectedItem()).severity);

        ChangeTrace trace = session.trace();
        List<Integer> logicColumns = new ArrayList<>(), analogColumns = new ArrayList<>();
        for (int c = 0; c < trace.columns().size(); c++) {
            if (trace.isAnalog(c)) analogColumns.add(c);
            else logicColumns.add(c);
        }
        view = new TimeView(session.simulation().engine().stepPs());
        view.follow(follow.isSelected());
        logic = new LogicPanel(runner, trace, view, logicColumns, session.labels());
        scope = new ScopePanel(runner, trace, view, analogColumns, session.labels());
        events = new EventPanel(runner, view);

        JComponent traces;
        JComponent logicBox = titled("Analizzatore logico", new JScrollPane(logic));
        JComponent scopeBox = titled("Oscilloscopio", new JScrollPane(scope));
        if (logic.isEmpty()) traces = scopeBox;
        else if (scope.isEmpty()) traces = logicBox;
        else {
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logicBox, scopeBox);
            split.setResizeWeight(0.55);
            traces = split;
        }
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, traces,
                titled("Eventi  (un clic porta il cursore all'istante dell'evento)", events));
        left.setResizeWeight(1.0);
        left.setDividerLocation(860 - 230);

        cpus.clear();
        JComponent main = left;
        if (!session.cpus().isEmpty()) {
            JPanel column = new JPanel(new GridLayout(0, 1, 0, 4));
            for (CpuWatch w : session.cpus()) {
                CpuPanel p = new CpuPanel(runner, w, view);
                cpus.add(p);
                column.add(titled(w.label() + "  (clic: vai all'istante)", p));
            }
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, column);
            split.setResizeWeight(1.0);
            split.setDividerLocation(1280 - 360);
            main = split;
        }
        center.removeAll();
        center.add(main, BorderLayout.CENTER);
        center.revalidate();
        center.repaint();
    }

    private void refresh()
    {
        long now = session.nowPs();
        Runner runner = session.runner();
        boolean running = runner.running();
        playPause.setText(running ? "‖  Pausa" : "▶  Avvia");
        state.setText(running ? "IN CORSA" : "FERMA");
        state.setForeground(running ? RUNNING : STOPPED);
        String speedText = running ? "   (" + Quantities.time(Math.round(runner.measuredSpeed())) + " al secondo)" : "";
        time.setText("t = " + Quantities.time(now) + "   " + runner.status() + speedText);
        cursor.setText(view.cursorPs() >= 0
                ? "cursore a " + Quantities.time(view.cursorPs()) + "  (" + Quantities.time(Math.abs(now - view.cursorPs())) + " fa)"
                : "rotella: zoom   trascina: sposta   clic: cursore   tasto destro: togli cursore");
        if (follow.isSelected() != view.following()) follow.setSelected(view.following());
        events.refresh();
        for (CpuPanel p : cpus) p.refresh();
        logic.repaint();
        scope.repaint();
    }

    // ------------------------------------------------------------ le scelte

    /** Una velocità: quanti ps simulati per secondo reale (0: la massima). */
    private static final class Speed
    {
        static final Speed[] CHOICES = {
                new Speed("massima", 0),
                new Speed("1 ms al secondo", 1_000_000_000L),
                new Speed("100 µs al secondo", 100_000_000L),
                new Speed("10 µs al secondo", 10_000_000L),
                new Speed("1 µs al secondo", 1_000_000L),
                new Speed("100 ns al secondo", 100_000L),
                new Speed("10 ns al secondo", 10_000L),
        };

        final String label;
        final long psPerSecond;

        Speed(String label, long psPerSecond)
        {
            this.label = label;
            this.psPerSecond = psPerSecond;
        }

        @Override public String toString() { return label; }
    }

    /** Su quale gravità fermarsi (null: mai). */
    private static final class StopOn
    {
        final Severity severity;

        StopOn(Severity severity)  { this.severity = severity; }

        static StopOn[] choices()
        {
            Severity[] all = Severity.values();
            StopOn[] c = new StopOn[all.length + 1];
            c[0] = new StopOn(null);
            for (int i = 0; i < all.length; i++) c[i + 1] = new StopOn(all[all.length - 1 - i]);
            return c;
        }

        @Override
        public String toString()
        {
            if (severity == null) return "mai";
            return severity == Severity.KABOOM ? severity.sound() : severity.sound() + " o più grave";
        }
    }

    // ------------------------------------------------------------ gli ascoltatori

    private final class Build implements Runnable
    {
        @Override public void run() { build(); }
    }

    private final class Refresh implements ActionListener
    {
        @Override public void actionPerformed(ActionEvent e) { refresh(); }
    }

    private final class PlayPause implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (session.runner().running()) session.runner().pause();
            else session.runner().start();
            refresh();
        }
    }

    private final class Step implements ActionListener
    {
        @Override public void actionPerformed(ActionEvent e) { session.runner().step(); }
    }

    private final class Reset implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            session.close();
            follow.setSelected(true);                 // da capo: si riparte guardando il presente
            startSession();
            refresh();
        }
    }

    private final class Advance implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            try {
                session.runner().advance(Quantities.parseTime(advance.getText()));
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Avanza", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private final class SpeedChoice implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            session.runner().speed(((Speed) speed.getSelectedItem()).psPerSecond);
        }
    }

    private final class StopChoice implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            session.runner().stopOn(((StopOn) stopOn.getSelectedItem()).severity);
        }
    }

    private final class Follow implements ActionListener
    {
        @Override public void actionPerformed(ActionEvent e) { view.follow(follow.isSelected()); }
    }

    private final class Fit implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            view.fit(session.nowPs());
        }
    }

    private final class Zoom implements ActionListener
    {
        private final double factor;

        Zoom(double factor) { this.factor = factor; }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int width = Math.max(1, Math.max(logic.getWidth(), scope.getWidth()) - TimePanel.NAMES);
            view.zoom(factor, view.following() ? width : width / 2);
        }
    }

    private final class Closing extends WindowAdapter
    {
        @Override
        public void windowClosed(WindowEvent e)
        {
            timer.stop();
            session.close();
        }
    }
}
