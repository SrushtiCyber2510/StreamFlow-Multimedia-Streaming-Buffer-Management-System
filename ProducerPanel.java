import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProducerPanel extends JPanel {
    private final ProducerTask producerTask;
    private final SharedBuffer sharedBuffer;

    private final JLabel lblStatus = new JLabel("STOPPED", SwingConstants.CENTER);
    private final JLabel lblThreadStatus = new JLabel("Status: Stopped", SwingConstants.CENTER);
    private final JPanel blockedAlertCard = new JPanel(new BorderLayout());
    private final JLabel lblBlockedAlert = new JLabel("⚠️ BUFFER FULL — PRODUCER WAITING (BLOCKED VIA MONITORS)", SwingConstants.CENTER);

    private final JLabel lblCurrentChunk = createMetricValue();
    private final JLabel lblProductionTime = createMetricValue();
    private final JLabel lblTotalProduced = createMetricValue();
    private final JLabel lblSpeed = createMetricValue();
    private final JLabel lblWaitEvents = createMetricValue();

    private final ProducerAnimationCanvas animationCanvas = new ProducerAnimationCanvas();
    private final DefaultTableModel historyTableModel;

    public ProducerPanel(ProducerTask producerTask, SharedBuffer sharedBuffer) {
        this.producerTask = producerTask;
        this.sharedBuffer = sharedBuffer;

        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(11, 15, 25));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Streaming Server Dashboard (Producer Process)");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JPanel statusBadgeGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        statusBadgeGroup.setOpaque(false);

        lblStatus.setOpaque(true);
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblStatus.setPreferredSize(new Dimension(100, 28));

        lblThreadStatus.setOpaque(true);
        lblThreadStatus.setBackground(new Color(18, 26, 43));
        lblThreadStatus.setForeground(Color.CYAN);
        lblThreadStatus.setFont(new Font("Monospaced", Font.BOLD, 11));
        lblThreadStatus.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
        lblThreadStatus.setPreferredSize(new Dimension(160, 28));

        statusBadgeGroup.add(lblThreadStatus);
        statusBadgeGroup.add(lblStatus);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(statusBadgeGroup, BorderLayout.EAST);

        blockedAlertCard.setBackground(new Color(245, 158, 11, 40));
        blockedAlertCard.setBorder(BorderFactory.createLineBorder(new Color(245, 158, 11), 1));
        blockedAlertCard.setPreferredSize(new Dimension(0, 32));
        lblBlockedAlert.setForeground(new Color(245, 158, 11));
        lblBlockedAlert.setFont(new Font("SansSerif", Font.BOLD, 12));
        blockedAlertCard.add(lblBlockedAlert, BorderLayout.CENTER);
        blockedAlertCard.setVisible(false);

        JPanel headerContainer = new JPanel(new BorderLayout(6, 6));
        headerContainer.setOpaque(false);
        headerContainer.add(topPanel, BorderLayout.NORTH);
        headerContainer.add(blockedAlertCard, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controlPanel.setBackground(new Color(18, 26, 43));
        controlPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 20)));

        JButton btnStart = createButton("Start Producer", new Color(139, 92, 246));
        JButton btnPause = createButton("Pause", new Color(59, 130, 246));
        JButton btnResume = createButton("Resume", new Color(16, 185, 129));
        JButton btnStop = createButton("Stop", new Color(239, 68, 68));

        JSlider speedSlider = new JSlider(500, 4000, 1500);
        speedSlider.setOpaque(false);
        JLabel speedLabel = new JLabel("1500 ms");
        speedLabel.setForeground(Color.WHITE);

        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            speedLabel.setText(val + " ms");
            producerTask.setIntervalMs(val);
        });

        btnStart.addActionListener(e -> producerTask.start());
        btnPause.addActionListener(e -> producerTask.pause());
        btnResume.addActionListener(e -> producerTask.resume());
        btnStop.addActionListener(e -> producerTask.stop());

        controlPanel.add(btnStart);
        controlPanel.add(btnPause);
        controlPanel.add(btnResume);
        controlPanel.add(btnStop);

        JLabel lblSpeedLabel = new JLabel("  Speed Interval:");
        lblSpeedLabel.setForeground(Color.LIGHT_GRAY);
        controlPanel.add(lblSpeedLabel);
        controlPanel.add(speedSlider);
        controlPanel.add(speedLabel);

        JPanel metricsPanel = new JPanel(new GridLayout(1, 5, 8, 8));
        metricsPanel.setOpaque(false);

        metricsPanel.add(createCard("Current Chunk", lblCurrentChunk));
        metricsPanel.add(createCard("Time Taken", lblProductionTime));
        metricsPanel.add(createCard("Total Produced", lblTotalProduced));
        metricsPanel.add(createCard("Production Interval", lblSpeed));
        metricsPanel.add(createCard("Buffer Full Waits", lblWaitEvents));

        JPanel animationPanel = new JPanel(new BorderLayout());
        animationPanel.setBackground(new Color(18, 26, 43));
        animationPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 30)),
                "Server ➔ Shared Buffer Transmission Flow", 0, 0, new Font("SansSerif", Font.BOLD, 11), Color.GRAY
        ));
        animationPanel.setPreferredSize(new Dimension(0, 90));
        animationPanel.add(animationCanvas, BorderLayout.CENTER);

        String[] columnNames = {"Chunk ID", "Chunk Size", "Production Time", "Generated Timestamp"};
        historyTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable historyTable = new JTable(historyTableModel);
        historyTable.setBackground(new Color(10, 14, 23));
        historyTable.setForeground(Color.CYAN);
        historyTable.setGridColor(new Color(255, 255, 255, 20));
        historyTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        historyTable.setRowHeight(22);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            historyTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        historyTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        historyTable.getTableHeader().setBackground(new Color(18, 26, 43));
        historyTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollTable = new JScrollPane(historyTable);
        scrollTable.setPreferredSize(new Dimension(0, 140));
        scrollTable.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 30)),
                "Production History Log", 0, 0, new Font("SansSerif", Font.BOLD, 11), Color.GRAY
        ));

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(controlPanel, BorderLayout.NORTH);

        JPanel midContent = new JPanel(new BorderLayout(8, 8));
        midContent.setOpaque(false);
        midContent.add(metricsPanel, BorderLayout.NORTH);
        midContent.add(animationPanel, BorderLayout.CENTER);

        centerPanel.add(midContent, BorderLayout.CENTER);
        centerPanel.add(scrollTable, BorderLayout.SOUTH);

        add(headerContainer, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void updateUIState() {
        String status = producerTask.getStatusString();

        switch (status) {
            case "Running":
                setStatus("RUNNING", new Color(16, 185, 129));
                blockedAlertCard.setVisible(false);
                break;
            case "Waiting":
                setStatus("WAITING", new Color(245, 158, 11));
                blockedAlertCard.setVisible(true);
                break;
            case "Paused":
                setStatus("PAUSED", new Color(59, 130, 246));
                blockedAlertCard.setVisible(false);
                break;
            default:
                setStatus("STOPPED", new Color(239, 68, 68));
                blockedAlertCard.setVisible(false);
                break;
        }

        lblThreadStatus.setText("Status: " + status);

        Chunk current = producerTask.getCurrentChunk();
        lblCurrentChunk.setText(current != null && producerTask.isRunning() ? current.getId() : "None");
        lblProductionTime.setText(producerTask.getLastChunkProductionTime() > 0 ? producerTask.getLastChunkProductionTime() + " ms" : "--");
        lblTotalProduced.setText(String.valueOf(sharedBuffer.getTotalProduced()));
        lblSpeed.setText(producerTask.getIntervalMs() + " ms");
        lblWaitEvents.setText(String.valueOf(sharedBuffer.getProducerWaitEvents()));

        animationCanvas.updateState(producerTask.isRunning() && !producerTask.isPaused(), current != null ? current.getId() : "CH-000", status.equals("Waiting"));

        historyTableModel.setRowCount(0);
        List<ProducerTask.ProductionRecord> records = producerTask.getProductionHistory();
        for (ProducerTask.ProductionRecord record : records) {
            historyTableModel.addRow(new Object[]{
                    record.chunkId,
                    record.size,
                    record.timeTakenMs + " ms",
                    record.timestamp
            });
        }
    }

    private void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
        lblStatus.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        return btn;
    }

    private JPanel createCard(String title, JLabel val) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBackground(new Color(18, 26, 43));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 20)),
                new EmptyBorder(8, 8, 8, 8)
        ));
        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(new Font("SansSerif", Font.BOLD, 9));
        t.setForeground(new Color(156, 163, 175));
        p.add(t, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private JLabel createMetricValue() {
        JLabel label = new JLabel("0");
        label.setFont(new Font("SansSerif", Font.BOLD, 17));
        label.setForeground(Color.WHITE);
        return label;
    }

    private static class ProducerAnimationCanvas extends JPanel {
        private int animX = 0;
        private boolean isActive = false;
        private boolean isBlocked = false;
        private String chunkId = "CH-000";

        public void updateState(boolean isActive, String chunkId, boolean isBlocked) {
            this.isActive = isActive;
            this.chunkId = chunkId;
            this.isBlocked = isBlocked;

            if (isActive && !isBlocked) {
                animX = (animX + 15) % Math.max(1, (getWidth() - 220));
            } else if (isBlocked) {
                animX = getWidth() - 230;
            } else {
                animX = 0;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setColor(new Color(10, 14, 23));
            g2.fillRect(0, 0, width, height);

            g2.setColor(new Color(139, 92, 246));
            g2.fillRoundRect(20, 10, 95, 38, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString("SERVER", 44, 33);

            g2.setColor(new Color(6, 182, 212));
            g2.fillRoundRect(width - 115, 10, 95, 38, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString("BUFFER", width - 90, 33);

            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6}, 0));
            g2.drawLine(120, 29, width - 120, 29);

            if (isActive || isBlocked) {
                int posX = 125 + animX;
                g2.setColor(isBlocked ? new Color(245, 158, 11) : new Color(16, 185, 129));
                g2.fillRoundRect(posX, 13, 65, 32, 8, 8);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString(chunkId, posX + 10, 33);
            }
        }
    }
}