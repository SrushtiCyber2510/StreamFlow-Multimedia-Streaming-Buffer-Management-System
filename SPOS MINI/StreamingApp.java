import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StreamingApp extends JFrame implements StreamListener {

    // ---------- Theme ----------
    private final Color backgroundColor = new Color(11, 15, 28);
    private final Color panelColor      = new Color(22, 29, 48);
    private final Color fieldColor      = new Color(31, 41, 66);
    private final Color borderColor     = new Color(42, 54, 82);
    private final Color accentColor     = new Color(56, 189, 248);
    private final Color greenColor      = new Color(52, 211, 153);
    private final Color orangeColor     = new Color(251, 146, 60);
    private final Color purpleColor     = new Color(167, 139, 250);
    private final Color redColor        = new Color(248, 113, 113);
    private final Color textColor       = new Color(238, 242, 250);
    private final Color mutedColor      = new Color(142, 158, 186);
    private final Color logBgColor      = new Color(9, 13, 24);

    private static final String FONT = "SansSerif";

    // ---------- Components ----------
    private JTextField bufferField;
    private JTextField chunksField;
    private JTextField productionField;
    private JTextField playbackField;

    private JComboBox<String> mediaTypeBox;
    private JSpinner deviceSpinner;

    private JLabel bufferLabel;
    private JLabel producedLabel;
    private JLabel consumedLabel;
    private JLabel producerWaitLabel;
    private JLabel consumerWaitLabel;
    private JLabel statusLabel;

    private JProgressBar bufferBar;

    private JTextPane logArea;

    private JButton startButton;
    private JButton stopButton;
    private JButton clearButton;

    // ---------- Logic fields (unchanged) ----------
    private SharedBuffer buffer;
    private StreamingServer server;
    private final List<UserDevice> devices = new ArrayList<>();

    private Timer timer;

    private int completedDevices = 0;
    private int totalDevices = 0;

    public StreamingApp() {
        setTitle("StreamFlow | Buffer Management System");
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        setSize(Math.min(1280, screen.width), Math.min(760, screen.height));
        setMinimumSize(new Dimension(Math.min(1000, screen.width), 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(backgroundColor);

        createGUI();
    }

    private void createGUI() {
        setLayout(new BorderLayout());

        // ================= HEADER =================
        JPanel header = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(24, 34, 66),
                        getWidth(), getHeight(), backgroundColor));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(borderColor);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 30, 18, 30));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("StreamFlow");
        title.setFont(new Font(FONT, Font.BOLD, 28));
        title.setForeground(textColor);

        JLabel subtitle = new JLabel("Multimedia Streaming Buffer Management  •  Producer-Consumer");
        subtitle.setFont(new Font(FONT, Font.PLAIN, 13));
        subtitle.setForeground(mutedColor);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(subtitle);

        JLabel badge = new JLabel("●  SIMULATOR");
        badge.setFont(new Font(FONT, Font.BOLD, 11));
        badge.setForeground(greenColor);
        RoundedPanel badgePill = new RoundedPanel(new Color(16, 44, 46), new Color(36, 98, 84), 30);
        badgePill.setLayout(new BorderLayout());
        badgePill.setBorder(new EmptyBorder(7, 16, 7, 16));
        badgePill.add(badge, BorderLayout.CENTER);

        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(badgePill);

        header.add(new LogoMark(), BorderLayout.WEST);
        header.add(titlePanel, BorderLayout.CENTER);
        header.add(badgeWrap, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ================= CONTENT =================
        JPanel content = new JPanel(new BorderLayout(20, 0));
        content.setBackground(backgroundColor);
        content.setBorder(new EmptyBorder(20, 30, 24, 30));

        // ---------- Configuration ----------
        JPanel configPanel = createCard();
        configPanel.setLayout(new BorderLayout(0, 14));
        configPanel.setBorder(new EmptyBorder(18, 22, 20, 22));
        configPanel.add(createSectionTitle("CONFIGURATION", accentColor), BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridLayout(2, 3, 20, 16));
        fieldsPanel.setOpaque(false);

        bufferField = new RoundedTextField("e.g. 5");
        chunksField = new RoundedTextField("e.g. 20");
        productionField = new RoundedTextField("e.g. 300");
        playbackField = new RoundedTextField("e.g. 500");

        mediaTypeBox = new RoundedComboBox(new String[]{"Video", "Audio"});
        deviceSpinner = new RoundedSpinner(new SpinnerNumberModel(1, 1, 5, 1));

        styleField(bufferField);
        styleField(chunksField);
        styleField(productionField);
        styleField(playbackField);
        styleComboBox(mediaTypeBox);
        styleSpinner(deviceSpinner);

        fieldsPanel.add(createInputGroup("Buffer Capacity", bufferField));
        fieldsPanel.add(createInputGroup("Total Media Chunks", chunksField));
        fieldsPanel.add(createInputGroup("Media Type", mediaTypeBox));
        fieldsPanel.add(createInputGroup("Production Delay (ms)", productionField));
        fieldsPanel.add(createInputGroup("Playback Delay (ms)", playbackField));
        fieldsPanel.add(createInputGroup("User Devices", deviceSpinner));

        configPanel.add(fieldsPanel, BorderLayout.CENTER);

        // ---------- Dashboard ----------
        JPanel dashboard = new JPanel(new BorderLayout(0, 12));
        dashboard.setBackground(backgroundColor);
        dashboard.add(createSectionTitle("LIVE DASHBOARD", greenColor), BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 14, 14));
        statsPanel.setBackground(backgroundColor);

        bufferLabel = createStatValue("0 / 0", accentColor);
        producedLabel = createStatValue("0", textColor);
        consumedLabel = createStatValue("0", textColor);
        producerWaitLabel = createStatValue("0", orangeColor);
        consumerWaitLabel = createStatValue("0", purpleColor);
        statusLabel = createStatValue("READY", greenColor);

        bufferBar = new JProgressBar(0, 100);
        bufferBar.setValue(0);
        bufferBar.setBorderPainted(false);
        bufferBar.setBackground(fieldColor);
        bufferBar.setForeground(accentColor);
        bufferBar.setUI(new SlimBarUI());
        bufferBar.setPreferredSize(new Dimension(10, 8));

        statsPanel.add(createBufferCard());
        statsPanel.add(createStatCard("PRODUCED CHUNKS", producedLabel, greenColor));
        statsPanel.add(createStatCard("CONSUMED CHUNKS", consumedLabel, accentColor));
        statsPanel.add(createStatCard("PRODUCER WAITS", producerWaitLabel, orangeColor));
        statsPanel.add(createStatCard("CONSUMER WAITS", consumerWaitLabel, purpleColor));
        statsPanel.add(createStatCard("SYSTEM STATUS", statusLabel, greenColor));

        dashboard.add(statsPanel, BorderLayout.CENTER);

        // ---------- Buttons ----------
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actions.setBackground(backgroundColor);
        actions.setBorder(new EmptyBorder(2, 0, 0, 0));

        startButton = createButton("▶   Start", greenColor, true);
        stopButton = createButton("■   Stop", redColor, false);
        clearButton = createButton("Clear Log", mutedColor, false);

        stopButton.setEnabled(false);

        actions.add(startButton);
        actions.add(stopButton);
        actions.add(clearButton);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setBackground(backgroundColor);
        top.add(configPanel, BorderLayout.NORTH);
        top.add(dashboard, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

                // ---------- Activity log ----------
        JPanel logPanel = createCard();
        logPanel.setLayout(new BorderLayout(0, 12));
        logPanel.setBorder(new EmptyBorder(16, 20, 20, 20));
        logPanel.setMinimumSize(new Dimension(320, 200));

        JPanel logHeader = new JPanel(new BorderLayout(0, 6));
        logHeader.setOpaque(false);
        logHeader.add(createSectionTitle("ACTIVITY LOG", purpleColor), BorderLayout.NORTH);
        JPanel legendRow = createLegend();
        ((FlowLayout) legendRow.getLayout()).setAlignment(FlowLayout.LEFT);
        logHeader.add(legendRow, BorderLayout.CENTER);

        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setBackground(logBgColor);
        logArea.setForeground(new Color(190, 208, 230));
        logArea.setCaretColor(accentColor);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(10, 14, 10, 14));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(borderColor));
        scrollPane.getViewport().setBackground(logBgColor);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scrollPane.getVerticalScrollBar().setBackground(logBgColor);

        logPanel.add(logHeader, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        // Left column: controls (scrolls only if the window is very short)
        JScrollPane leftScroll = new JScrollPane(top,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(null);
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setBackground(backgroundColor);
        leftScroll.getVerticalScrollBar().setUnitIncrement(20);
        leftScroll.setPreferredSize(new Dimension(620, 100));

        // Right column: Activity Log, always visible at full height
        content.add(leftScroll, BorderLayout.WEST);
        content.add(logPanel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // ---------- Button actions (unchanged) ----------
        startButton.addActionListener(e -> startStreaming());
        stopButton.addActionListener(e -> stopStreaming());
        clearButton.addActionListener(e -> clearLog());

        timer = new Timer(200, e -> updateStats());
    }

    // ================= UI HELPERS =================

    private JPanel createCard() {
        return new RoundedPanel(panelColor, borderColor, 16);
    }

    private JLabel createSectionTitle(String text, Color dot) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.BOLD, 12));
        label.setForeground(textColor);
        label.setIcon(new DotIcon(dot, 8));
        label.setIconTextGap(9);
        return label;
    }

    private JPanel createLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legend.setOpaque(false);
        legend.add(legendItem("Producer", greenColor));
        legend.add(legendItem("Buffer", orangeColor));
        legend.add(legendItem("Consumer", accentColor));
        return legend;
    }

    private JLabel legendItem(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(mutedColor);
        l.setFont(new Font(FONT, Font.PLAIN, 11));
        l.setIcon(new DotIcon(color, 7));
        l.setIconTextGap(6);
        return l;
    }

    private JPanel createInputGroup(String title, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 7));
        group.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font(FONT, Font.PLAIN, 12));
        label.setForeground(mutedColor);

        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private void styleField(JTextField field) {
        field.setForeground(textColor);
        field.setCaretColor(accentColor);
        field.setDisabledTextColor(mutedColor);
        field.setSelectionColor(new Color(56, 189, 248, 110));
        field.setFont(new Font(FONT, Font.PLAIN, 14));
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setForeground(textColor);
        combo.setFont(new Font(FONT, Font.PLAIN, 14));
        combo.setFocusable(false);

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new ChevronButton(true);
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // background is painted by RoundedComboBox
            }
        });
        combo.setBorder(new EmptyBorder(1, 4, 1, 1));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(8, 12, 8, 12));
                l.setOpaque(index != -1);
                l.setBackground(isSelected ? accentColor : fieldColor);
                l.setForeground(isSelected ? backgroundColor : textColor);
                return l;
            }
        });

        Object popup = combo.getUI().getAccessibleChild(combo, 0);
        if (popup instanceof BasicComboPopup) {
            BasicComboPopup p = (BasicComboPopup) popup;
            p.setBorder(BorderFactory.createLineBorder(borderColor));
            p.getList().setBackground(fieldColor);
        }
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font(FONT, Font.PLAIN, 14));
        spinner.setOpaque(false);
        spinner.setBorder(new EmptyBorder(1, 1, 1, 1));

        spinner.setUI(new BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                ChevronButton b = new ChevronButton(false);
                installNextButtonListeners(b);
                return b;
            }

            @Override
            protected Component createPreviousButton() {
                ChevronButton b = new ChevronButton(true);
                installPreviousButtonListeners(b);
                return b;
            }
        });

        JComponent editor = spinner.getEditor();
        editor.setOpaque(false);

        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setOpaque(false);
            textField.setForeground(textColor);
            textField.setCaretColor(accentColor);
            textField.setBorder(new EmptyBorder(9, 12, 9, 6));
        }
    }

    private JLabel createStatValue(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font(FONT, Font.BOLD, 28));
        return label;
    }

    private JPanel createStatCard(String title, JLabel value, Color accent) {
        RoundedPanel card = new RoundedPanel(panelColor, borderColor, 16);
        card.setAccent(accent);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(16, 20, 16, 18));

        JLabel heading = new JLabel(title);
        heading.setForeground(mutedColor);
        heading.setFont(new Font(FONT, Font.BOLD, 10));

        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    // Buffer card = value + progress bar
    private JPanel createBufferCard() {
        RoundedPanel card = new RoundedPanel(panelColor, borderColor, 16);
        card.setAccent(accentColor);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(16, 20, 16, 18));

        JLabel heading = new JLabel("BUFFER USAGE");
        heading.setForeground(mutedColor);
        heading.setFont(new Font(FONT, Font.BOLD, 10));

        card.add(heading, BorderLayout.NORTH);
        card.add(bufferLabel, BorderLayout.CENTER);
        card.add(bufferBar, BorderLayout.SOUTH);
        return card;
    }

    private JButton createButton(String text, Color color, boolean primary) {
        return new PillButton(text, color, primary);
    }

    // Colours the status label according to its text
    private void setStatus(String text) {
        statusLabel.setText(text);
        switch (text) {
            case "RUNNING":
                statusLabel.setForeground(accentColor);
                break;
            case "DRAINING BUFFER":
                statusLabel.setForeground(orangeColor);
                break;
            default:
                statusLabel.setForeground(greenColor);
        }
    }

    // Appends one coloured line to the activity log
    private void appendLogLine(String message) {
        Color c;
        if (message.startsWith("=====")) {
            c = accentColor;
        } else if (message.startsWith("BUFFER FULL")) {
            c = orangeColor;
        } else if (message.startsWith("BUFFER EMPTY")) {
            c = purpleColor;
        } else if (message.startsWith("SERVER")) {
            c = greenColor;
        } else if (message.startsWith("Device")) {
            c = new Color(125, 211, 252);
        } else if (message.startsWith("CONFIG")) {
            c = mutedColor;
        } else {
            c = new Color(190, 208, 230);
        }

        StyledDocument doc = logArea.getStyledDocument();
        SimpleAttributeSet time = new SimpleAttributeSet();
        StyleConstants.setForeground(time, new Color(86, 100, 130));
        SimpleAttributeSet body = new SimpleAttributeSet();
        StyleConstants.setForeground(body, c);
        if (message.startsWith("=====")) {
            StyleConstants.setBold(body, true);
        }

        try {
            doc.insertString(doc.getLength(),
                    new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  ", time);
            doc.insertString(doc.getLength(), message + "\n", body);
        } catch (BadLocationException ignored) {
            // cannot happen: offsets come from the document itself
        }
    }

    // ================= START STREAMING =================
    private void startStreaming() {
        try {
            int bufferCapacity = Integer.parseInt(bufferField.getText().trim());
            int totalChunks = Integer.parseInt(chunksField.getText().trim());
            int productionDelay = Integer.parseInt(productionField.getText().trim());
            int playbackDelay = Integer.parseInt(playbackField.getText().trim());

            int deviceCount = (Integer) deviceSpinner.getValue();
            String mediaType = (String) mediaTypeBox.getSelectedItem();

            if (bufferCapacity <= 0 || totalChunks <= 0
                    || productionDelay < 0 || playbackDelay < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Capacity and chunks must be greater than zero. Delays cannot be negative.",
                        "Invalid Input",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            stopStreaming();

            buffer = new SharedBuffer(bufferCapacity, this);

            completedDevices = 0;
            totalDevices = deviceCount;
            devices.clear();
            logArea.setText("");

            log("========== NEW STREAMING SESSION ==========");
            log("CONFIG -> Buffer: " + bufferCapacity
                    + ", Chunks: " + totalChunks
                    + ", Media: " + mediaType
                    + ", Devices: " + deviceCount);

            server = new StreamingServer(
                    buffer, totalChunks, productionDelay,
                    mediaType, deviceCount, this
            );

            for (int i = 1; i <= deviceCount; i++) {
                UserDevice device = new UserDevice(
                        "Device-" + i,
                        buffer,
                        playbackDelay,
                        this
                );
                devices.add(device);
            }

            setConfigurationEnabled(false);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            setStatus("RUNNING");

            server.start();

            for (UserDevice device : devices) {
                device.start();
            }

            timer.start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numbers in all required fields.",
                    "Missing or Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    // ================= STOP STREAMING =================
    private void stopStreaming() {
        if (server != null) {
            server.stopServer();
        }

        for (UserDevice device : devices) {
            device.stopDevice();
        }

        if (timer != null) {
            timer.stop();
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        setConfigurationEnabled(true);
    }

    private void setConfigurationEnabled(boolean enabled) {
        bufferField.setEnabled(enabled);
        chunksField.setEnabled(enabled);
        productionField.setEnabled(enabled);
        playbackField.setEnabled(enabled);
        mediaTypeBox.setEnabled(enabled);
        deviceSpinner.setEnabled(enabled);
    }

    // ================= LOG =================
    private void clearLog() {
        logArea.setText("");
        logArea.setCaretPosition(0);
        logArea.repaint();
    }

    @Override
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            appendLogLine(message);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ================= UPDATE DASHBOARD =================
    @Override
    public void updateStats() {
        SwingUtilities.invokeLater(() -> {
            if (buffer == null) {
                return;
            }

            int size = buffer.getBufferSize();
            int cap = buffer.getCapacity();

            bufferLabel.setText(size + " / " + cap);
            bufferBar.setValue(cap > 0 ? (size * 100) / cap : 0);

            producedLabel.setText(String.valueOf(buffer.getProducedCount()));
            consumedLabel.setText(String.valueOf(buffer.getConsumedCount()));
            producerWaitLabel.setText(String.valueOf(buffer.getProducerWaits()));
            consumerWaitLabel.setText(String.valueOf(buffer.getConsumerWaits()));
        });
    }

    // ================= PRODUCTION COMPLETED =================
    @Override
    public void productionCompleted() {
        SwingUtilities.invokeLater(() -> setStatus("DRAINING BUFFER"));
    }

    // ================= DEVICE COMPLETED =================
    @Override
    public void deviceCompleted(String deviceName) {
        SwingUtilities.invokeLater(() -> {
            completedDevices++;

            log(deviceName + " -> Consumer thread finished.");

            if (completedDevices >= totalDevices) {
                setStatus("COMPLETED");
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                setConfigurationEnabled(true);

                if (timer != null) {
                    timer.stop();
                }

                updateStats();
                log("========== STREAMING COMPLETED ==========");
            }
        });
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName()
                );
            } catch (Exception ignored) {
                // Use the default look and feel if unavailable.
            }

            // Dark tooltips / option dialogs to match the theme
            UIManager.put("OptionPane.background", new Color(22, 29, 48));
            UIManager.put("Panel.background", new Color(22, 29, 48));
            UIManager.put("OptionPane.messageForeground", new Color(238, 242, 250));

            StreamingApp app = new StreamingApp();
            app.setVisible(true);
        });
    }

    // =====================================================================
    //  Custom-painted UI components (visual only)
    // =====================================================================

    private static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Rounded card with optional coloured accent bar on the left edge. */
    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color line;
        private final int arc;
        private Color accent;

        RoundedPanel(Color fill, Color line, int arc) {
            this.fill = fill;
            this.line = line;
            this.arc = arc;
            setOpaque(false);
        }

        void setAccent(Color accent) {
            this.accent = accent;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
            g2.setColor(fill);
            g2.fill(shape);
            if (accent != null) {
                Shape oldClip = g2.getClip();
                g2.clip(shape);
                g2.setColor(accent);
                g2.fillRect(0, 0, 4, h + 1);
                g2.setClip(oldClip);
            }
            g2.setColor(line);
            g2.draw(shape);
            g2.dispose();
        }
    }

    /** Small coloured circle used in section titles and the log legend. */
    private static class DotIcon implements Icon {
        private final Color color;
        private final int size;

        DotIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Gradient app logo with a play symbol. */
    private static class LogoMark extends JComponent {
        LogoMark() {
            setPreferredSize(new Dimension(48, 48));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int s = Math.min(getWidth(), getHeight()) - 1;
            int y = (getHeight() - s) / 2;
            g2.setPaint(new GradientPaint(0, y, new Color(56, 189, 248),
                    s, y + s, new Color(139, 92, 246)));
            g2.fillRoundRect(0, y, s, s, 14, 14);

            Path2D tri = new Path2D.Float();
            float cx = s / 2f + 2, cy = y + s / 2f;
            tri.moveTo(cx - 8, cy - 11);
            tri.lineTo(cx + 12, cy);
            tri.lineTo(cx - 8, cy + 11);
            tri.closePath();
            g2.setColor(Color.WHITE);
            g2.fill(tri);
            g2.dispose();
        }
    }

    /** Rounded text input with an accent outline on focus. */
    private class RoundedTextField extends JTextField {
        private final String placeholder;

        RoundedTextField(String placeholder) {
            super();
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 13, 10, 13));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(isEnabled() ? fieldColor : new Color(25, 33, 54));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);

            // faint hint shown only while the field is empty
            if (getText().isEmpty()) {
                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                h.setColor(new Color(100, 114, 142));
                h.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = h.getFontMetrics();
                h.drawString(placeholder, in.left,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                h.dispose();
            }
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(isFocusOwner() ? accentColor : borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
        }
    }

    /** Rounded drop-down. */
    private class RoundedComboBox extends JComboBox<String> {
        RoundedComboBox(String[] items) {
            super(items);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(isEnabled() ? fieldColor : new Color(25, 33, 54));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
        }
    }

    /** Rounded number spinner. */
    private class RoundedSpinner extends JSpinner {
        RoundedSpinner(SpinnerModel model) {
            super(model);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(isEnabled() ? fieldColor : new Color(25, 33, 54));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
        }
    }

    /** Flat chevron button used by the combo box and spinner. */
    private class ChevronButton extends JButton {
        private final boolean down;

        ChevronButton(boolean down) {
            this.down = down;
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setPreferredSize(new Dimension(28, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(isEnabled()
                    ? (getModel().isRollover() ? accentColor : mutedColor)
                    : new Color(80, 92, 118));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int d = down ? 1 : -1;
            g2.drawLine(cx - 4, cy - 2 * d, cx, cy + 2 * d);
            g2.drawLine(cx, cy + 2 * d, cx + 4, cy - 2 * d);
            g2.dispose();
        }
    }

    /** Rounded action button with hover / pressed / disabled states. */
    private class PillButton extends JButton {
        private final Color color;
        private final boolean primary;
        private boolean hover;

        PillButton(String text, Color color, boolean primary) {
            super(text);
            this.color = color;
            this.primary = primary;
            setFont(new Font(FONT, Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(11, 26, 11, 26));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            boolean on = isEnabled();
            boolean pressed = getModel().isPressed();

            Color fg;
            if (!on) {
                g2.setColor(new Color(24, 32, 52));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(new Color(40, 51, 76));
                g2.drawRoundRect(0, 0, w, h, 14, 14);
                fg = new Color(88, 101, 128);
            } else if (primary) {
                Color base = pressed ? color.darker() : (hover ? color.brighter() : color);
                g2.setPaint(new GradientPaint(0, 0, base, 0, h, base.darker()));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                fg = new Color(6, 32, 28);
            } else {
                int alpha = pressed ? 60 : (hover ? 38 : 0);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(hover ? color : new Color(color.getRed(), color.getGreen(),
                        color.getBlue(), 150));
                g2.drawRoundRect(0, 0, w, h, 14, 14);
                fg = color;
            }

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(fg);
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    /** Rounded buffer-usage bar: blue, then orange and red as the buffer fills. */
    private class SlimBarUI extends BasicProgressBarUI {
        @Override
        protected void paintDeterminate(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = c.getWidth();
            int h = c.getHeight();

            g2.setColor(fieldColor);
            g2.fillRoundRect(0, 0, w, h, h, h);

            double pct = progressBar.getPercentComplete();
            if (pct > 0) {
                int fw = Math.max(h, (int) Math.round(w * pct));
                Color tone = pct >= 0.9 ? redColor : (pct >= 0.6 ? orangeColor : accentColor);
                g2.setPaint(new GradientPaint(0, 0, tone.brighter(), fw, 0, tone));
                g2.fillRoundRect(0, 0, fw, h, h, h);
            }
            g2.dispose();
        }
    }

    /** Slim dark scrollbar for the activity log. */
    private static class DarkScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(52, 66, 98);
            trackColor = new Color(9, 13, 24);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return emptyButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return emptyButton();
        }

        private JButton emptyButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(isThumbRollover() ? new Color(74, 92, 130) : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }
}
