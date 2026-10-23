import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ConsumerPanel extends JPanel {
    private final ConsumerTask consumerTask;
    private final SharedBuffer sharedBuffer;

    private final JLabel lblStatus = new JLabel("STOPPED", SwingConstants.CENTER);
    private final JPanel starvedAlertCard = new JPanel(new BorderLayout());
    private final JLabel lblStarvedAlert = new JLabel("⚠️ BUFFER STARVATION — CONSUMER WAITING (BUFFER EMPTY)", SwingConstants.CENTER);

    private final JLabel lblCurrentChunk = createMetricValue();
    private final JLabel lblTotalConsumed = createMetricValue();
    private final JLabel lblPlaybackSpeed = createMetricValue();
    private final JLabel lblUnderflowWaits = createMetricValue();
    private final JLabel lblStarvations = createMetricValue();

    private final JLabel lblPlayerState = new JLabel("STOPPED", SwingConstants.CENTER);
    private final JLabel lblChunkDetails = new JLabel("Playback Idle", SwingConstants.CENTER);
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel lblProgressPercent = new JLabel("0%", SwingConstants.RIGHT);

    private final ConsumerAnimationCanvas animationCanvas = new ConsumerAnimationCanvas();
    private final DefaultTableModel historyTableModel;

    public ConsumerPanel(ConsumerTask consumerTask, SharedBuffer sharedBuffer) {
        this.consumerTask = consumerTask;
        this.sharedBuffer = sharedBuffer;

        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(11, 15, 25));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Playback Device Dashboard (Consumer Process)");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JPanel statusBadgeGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        statusBadgeGroup.setOpaque(false);

        lblStatus.setOpaque(true);
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblStatus.setPreferredSize(new Dimension(100, 28));

        statusBadgeGroup.add(lblStatus);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(statusBadgeGroup, BorderLayout.EAST);

        starvedAlertCard.setBackground(new Color(239, 68, 68, 40));
        starvedAlertCard.setBorder(BorderFactory.createLineBorder(new Color(239, 68, 68), 1));
        starvedAlertCard.setPreferredSize(new Dimension(0, 32));
        lblStarvedAlert.setForeground(new Color(239, 68, 68));
        lblStarvedAlert.setFont(new Font("SansSerif", Font.BOLD, 12));
        starvedAlertCard.add(lblStarvedAlert, BorderLayout.CENTER);
        starvedAlertCard.setVisible(false);

        JPanel headerContainer = new JPanel(new BorderLayout(6, 6));
        headerContainer.setOpaque(false);
        headerContainer.add(topPanel, BorderLayout.NORTH);
        headerContainer.add(starvedAlertCard, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controlPanel.setBackground(new Color(18, 26, 43));
        controlPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 20)));

        JButton btnStart = createButton("Start Consumer", new Color(16, 185, 129));
        JButton btnPause = createButton("Pause", new Color(59, 130, 246));
        JButton btnResume = createButton("Resume", new Color(139, 92, 246));
        JButton btnStop = createButton("Stop", new Color(239, 68, 68));

        JSlider speedSlider = new JSlider(500, 4000, 2000);
        speedSlider.setOpaque(false);
        JLabel speedLabel = new JLabel("2000 ms");
        speedLabel.setForeground(Color.WHITE);

        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            speedLabel.setText(val + " ms");
            consumerTask.setIntervalMs(val);
        });

        btnStart.addActionListener(e -> consumerTask.start());
        btnPause.addActionListener(e -> consumerTask.pause());
        btnResume.addActionListener(e -> consumerTask.resume());
        btnStop.addActionListener(e -> consumerTask.stop());

        controlPanel.add(btnStart);
        controlPanel.add(btnPause);
        controlPanel.add(btnResume);
        controlPanel.add(btnStop);

        JLabel lblSpeedLabel = new JLabel("  Playback Speed:");
        lblSpeedLabel.setForeground(Color.LIGHT_GRAY);
        controlPanel.add(lblSpeedLabel);
        controlPanel.add(speedSlider);
        controlPanel.add(speedLabel);

        JPanel videoFrame = new JPanel(new BorderLayout());
        videoFrame.setBackground(new Color(0, 0, 0));
        videoFrame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(6, 182, 212), 2),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel screenTextContainer = new JPanel(new GridLayout(2, 1, 4, 4));
        screenTextContainer.setOpaque(false);

        lblPlayerState.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblPlayerState.setForeground(Color.GRAY);

        lblChunkDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblChunkDetails.setForeground(Color.LIGHT_GRAY);

        screenTextContainer.add(lblPlayerState);
        screenTextContainer.add(lblChunkDetails);

        videoFrame.add(screenTextContainer, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel(new BorderLayout(8, 0));
        progressPanel.setOpaque(false);
        progressPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

        progressBar.setForeground(new Color(6, 182, 212));
        progressBar.setBackground(new Color(10, 14, 23));
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 10));

        lblProgressPercent.setForeground(Color.CYAN);
        lblProgressPercent.setFont(new Font("SansSerif", Font.BOLD, 11));

        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(lblProgressPercent, BorderLayout.EAST);

        videoFrame.add(progressPanel, BorderLayout.SOUTH);

        JPanel metricsPanel = new JPanel(new GridLayout(1, 5, 8, 8));
        metricsPanel.setOpaque(false);

        metricsPanel.add(createCard("Current Chunk", lblCurrentChunk));
        metricsPanel.add(createCard("Total Consumed", lblTotalConsumed));
        metricsPanel.add(createCard("Playback Interval", lblPlaybackSpeed));
        metricsPanel.add(createCard("Buffer Empty Waits", lblUnderflowWaits));
        metricsPanel.add(createCard("Starvation Events", lblStarvations));

        JPanel animationPanel = new JPanel(new BorderLayout());
        animationPanel.setBackground(new Color(18, 26, 43));
        animationPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 30)),
                "Shared Buffer ➔ Media Player Playback Pipeline", 0, 0, new Font("SansSerif", Font.BOLD, 11), Color.GRAY
        ));
        animationPanel.setPreferredSize(new Dimension(0, 90));
        animationPanel.add(animationCanvas, BorderLayout.CENTER);

        String[] columnNames = {"Chunk ID", "Data Size", "Decode & Render Time", "Consumed Timestamp"};
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
        scrollTable.setPreferredSize(new Dimension(0, 130));
        scrollTable.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 30)),
                "Recent Playback History Log", 0, 0, new Font("SansSerif", Font.BOLD, 11), Color.GRAY
        ));

        JPanel mainContent = new JPanel(new BorderLayout(8, 8));
        mainContent.setOpaque(false);

        JPanel topSection = new JPanel(new BorderLayout(6, 6));
        topSection.setOpaque(false);
        topSection.add(controlPanel, BorderLayout.NORTH);
        topSection.add(videoFrame, BorderLayout.CENTER);

        JPanel midSection = new JPanel(new BorderLayout(6, 6));
        midSection.setOpaque(false);
        midSection.add(metricsPanel, BorderLayout.NORTH);
        midSection.add(animationPanel, BorderLayout.CENTER);

        mainContent.add(topSection, BorderLayout.NORTH);
        mainContent.add(midSection, BorderLayout.CENTER);
        mainContent.add(scrollTable, BorderLayout.SOUTH);

        add(headerContainer, BorderLayout.NORTH);
        add(mainContent, BorderLayout.CENTER);
    }

    public void updateUIState() {
        String status = consumerTask.getStatusString();

        switch (status) {
            case "Running":
                setStatus("RUNNING", new Color(16, 185, 129));
                starvedAlertCard.setVisible(false);
                break;
            case "Waiting":
                setStatus("WAITING", new Color(239, 68, 68));
                starvedAlertCard.setVisible(true);
                break;
            case "Paused":
                setStatus("PAUSED", new Color(59, 130, 246));
                starvedAlertCard.setVisible(false);
                break;
            default:
                setStatus("STOPPED", new Color(239, 68, 68));
                starvedAlertCard.setVisible(false);
                break;
        }

        Chunk current = consumerTask.getCurrentChunk();

        if (!consumerTask.isRunning() && !consumerTask.isPaused()) {
            lblPlayerState.setText("ENDED / STOPPED");
            lblPlayerState.setForeground(Color.GRAY);
            lblChunkDetails.setText("Click 'Start Consumer' to initialize playback");
        } else if (consumerTask.isPaused()) {
            lblPlayerState.setText("PAUSED");
            lblPlayerState.setForeground(new Color(245, 158, 11));
            lblChunkDetails.setText(current != null ? "Paused at " + current.getId() : "Stream paused");
        } else if (consumerTask.isWaiting()) {
            lblPlayerState.setText("BUFFERING...");
            lblPlayerState.setForeground(new Color(239, 68, 68));
            lblChunkDetails.setText("Buffer starvation: waiting for server chunks...");
        } else {
            lblPlayerState.setText("PLAYING VIDEO STREAM");
            lblPlayerState.setForeground(new Color(16, 185, 129));
            if (current != null) {
                lblChunkDetails.setText(String.format("Rendering Chunk: %s | Size: %.1f MB | Received: %s",
                        current.getId(), current.getSizeMb(), current.getTimestamp()));
            }
        }

        int totalConsumed = sharedBuffer.getTotalConsumed();
        int progress = Math.min(100, (int) (((double) totalConsumed / 50) * 100));
        progressBar.setValue(progress);
        lblProgressPercent.setText(progress + "%");

        lblCurrentChunk.setText(current != null && consumerTask.isRunning() ? current.getId() : "None");
        lblTotalConsumed.setText(String.valueOf(totalConsumed));
        lblPlaybackSpeed.setText(consumerTask.getIntervalMs() + " ms");
        lblUnderflowWaits.setText(String.valueOf(sharedBuffer.getConsumerWaitEvents()));
        lblStarvations.setText(String.valueOf(sharedBuffer.getConsumerWaitEvents()));

        animationCanvas.updateState(
                consumerTask.isRunning() && !consumerTask.isPaused(),
                current != null ? current.getId() : "CH-000",
                consumerTask.isWaiting()
        );

        historyTableModel.setRowCount(0);
        List<ConsumerTask.PlaybackRecord> records = consumerTask.getPlaybackHistory();
        for (ConsumerTask.PlaybackRecord record : records) {
            historyTableModel.addRow(new Object[]{
                    record.chunkId,
                    record.size,
                    record.renderTimeMs + " ms",
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

    private static class ConsumerAnimationCanvas extends JPanel {
        private int animX = 0;
        private boolean isActive = false;
        private boolean isWaiting = false;
        private String chunkId = "CH-000";

        public void updateState(boolean isActive, String chunkId, boolean isWaiting) {
            this.isActive = isActive;
            this.chunkId = chunkId;
            this.isWaiting = isWaiting;

            if (isActive && !isWaiting) {
                animX = (animX + 15) % Math.max(1, (getWidth() - 220));
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
            g2.drawString("BUFFER", 44, 33);

            g2.setColor(new Color(6, 182, 212));
            g2.fillRoundRect(width - 115, 10, 95, 38, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString("PLAYER", width - 90, 33);

            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6}, 0));
            g2.drawLine(120, 29, width - 120, 29);

            if (isActive && !isWaiting) {
                int posX = 125 + animX;
                g2.setColor(new Color(6, 182, 212));
                g2.fillRoundRect(posX, 13, 65, 32, 8, 8);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString(chunkId, posX + 10, 33);
            } else if (isWaiting) {
                g2.setColor(new Color(239, 68, 68));
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString("NO DATA CHUNKS IN BUFFER (BUFFERING)", width / 2 - 130, 33);
            }
        }
    }
}