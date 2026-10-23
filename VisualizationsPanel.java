import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VisualizationsPanel extends JPanel {
    private final SharedBuffer buffer;
    private final ProducerTask producerTask;
    private final ConsumerTask consumerTask;

    // Time-series history buffers (max 40 points)
    private final List<Integer> occupancyHistory = new ArrayList<>();
    private final List<Integer> producedHistory = new ArrayList<>();
    private final List<Integer> consumedHistory = new ArrayList<>();

    // 4 Clean Custom Canvases
    private final OccupancyGraphCanvas occupancyCanvas = new OccupancyGraphCanvas();
    private final BufferUtilizationCanvas utilizationCanvas = new BufferUtilizationCanvas();
    private final PlaybackBufferingCanvas playbackCanvas = new PlaybackBufferingCanvas();
    private final ProductionConsumptionCanvas prodConsCanvas = new ProductionConsumptionCanvas();

    // System Event Feed
    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JList<String> logList = new JList<>(logModel);

    public VisualizationsPanel(SharedBuffer buffer, ProducerTask producerTask, ConsumerTask consumerTask) {
        this.buffer = buffer;
        this.producerTask = producerTask;
        this.consumerTask = consumerTask;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(11, 15, 25));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel lblHeader = new JLabel("Real-Time Analytics & Streaming Visualizations");
        lblHeader.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblHeader.setForeground(Color.WHITE);

        JPanel gridPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        gridPanel.setOpaque(false);

        gridPanel.add(createCard("1. Buffer Occupancy Graph", occupancyCanvas));
        gridPanel.add(createCard("2. Buffer Utilization Gauge", utilizationCanvas));
        gridPanel.add(createCard("3. Playback Buffering Status", playbackCanvas));
        gridPanel.add(createCard("4. Production vs Consumption", prodConsCanvas));

        // Bottom Section: Live Event Log
        JPanel logContainer = new JPanel(new BorderLayout(6, 6));
        logContainer.setOpaque(false);
        logContainer.setPreferredSize(new Dimension(0, 150));

        JLabel lblLogTitle = new JLabel("System Event Feed:");
        lblLogTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblLogTitle.setForeground(Color.WHITE);

        logList.setBackground(new Color(10, 14, 23));
        logList.setForeground(new Color(6, 182, 212));
        logList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollLog = new JScrollPane(logList);
        scrollLog.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 20)));

        logContainer.add(lblLogTitle, BorderLayout.NORTH);
        logContainer.add(scrollLog, BorderLayout.CENTER);

        add(lblHeader, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);
        add(logContainer, BorderLayout.SOUTH);
    }

    public void updateUIState() {
        int capacity = buffer.getCapacity();
        int currentSize = buffer.getCurrentSize();
        int produced = buffer.getTotalProduced();
        int consumed = buffer.getTotalConsumed();

        occupancyHistory.add(currentSize);
        producedHistory.add(produced);
        consumedHistory.add(consumed);

        if (occupancyHistory.size() > 40) {
            occupancyHistory.remove(0);
            producedHistory.remove(0);
            consumedHistory.remove(0);
        }

        occupancyCanvas.updateData(occupancyHistory, capacity);
        utilizationCanvas.updateState(currentSize, capacity);
        playbackCanvas.updateState(producerTask, consumerTask, buffer);
        prodConsCanvas.updateData(producedHistory, consumedHistory);

        logModel.clear();
        for (String log : buffer.getLogs()) {
            logModel.addElement(log);
        }
    }

    private JPanel createCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(new Color(18, 26, 43));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 20)),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTitle.setForeground(new Color(156, 163, 175));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // --- 1. Buffer Occupancy Graph ---
    private static class OccupancyGraphCanvas extends JPanel {
        private List<Integer> history = new ArrayList<>();
        private int capacity = 10;

        public void updateData(List<Integer> history, int capacity) {
            this.history = new ArrayList<>(history);
            this.capacity = capacity;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(10, 14, 23));
            g2.fillRect(0, 0, w, h);

            if (history.size() < 2) return;

            int margin = 25;
            int chartW = w - 2 * margin;
            int chartH = h - 2 * margin;

            // Capacity Line
            g2.setColor(new Color(239, 68, 68, 120));
            g2.drawLine(margin, margin, w - margin, margin);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g2.drawString("Max Capacity (" + capacity + ")", w - margin - 80, margin - 4);

            // Graph Line
            g2.setColor(new Color(6, 182, 212));
            g2.setStroke(new BasicStroke(2));

            for (int i = 0; i < history.size() - 1; i++) {
                int x1 = margin + (i * chartW / 40);
                int y1 = h - margin - (history.get(i) * chartH / Math.max(1, capacity));
                int x2 = margin + ((i + 1) * chartW / 40);
                int y2 = h - margin - (history.get(i + 1) * chartH / Math.max(1, capacity));
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }

    // --- 2. Buffer Utilization ---
    private static class BufferUtilizationCanvas extends JPanel {
        private int current = 0;
        private int capacity = 10;

        public void updateState(int current, int capacity) {
            this.current = current;
            this.capacity = capacity;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(10, 14, 23));
            g2.fillRect(0, 0, w, h);

            int diameter = Math.min(w, h) - 24;
            int x = (w - diameter) / 2;
            int y = (h - diameter) / 2;

            int angle = (int) (((double) current / Math.max(1, capacity)) * 360);

            // Background Ring
            g2.setColor(new Color(30, 41, 59));
            g2.fillOval(x, y, diameter, diameter);

            // Color-Coded Utilization Arc
            g2.setColor(current == capacity ? new Color(245, 158, 11) : (current == 0 ? new Color(239, 68, 68) : new Color(6, 182, 212)));
            g2.fillArc(x, y, diameter, diameter, 90, -angle);

            // Cutout
            int innerD = diameter - 20;
            g2.setColor(new Color(10, 14, 23));
            g2.fillOval(x + 10, y + 10, innerD, innerD);

            // Center Text
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            int pct = (int) (((double) current / Math.max(1, capacity)) * 100);
            g2.drawString(pct + "%", w / 2 - 12, h / 2 + 4);
        }
    }

    // --- 3. Playback Buffering ---
    private static class PlaybackBufferingCanvas extends JPanel {
        private String consStatus = "Stopped";
        private String activeChunk = "CH-000";

        public void updateState(ProducerTask prod, ConsumerTask cons, SharedBuffer buf) {
            this.consStatus = cons.getStatusString();
            Chunk c = cons.getCurrentChunk();
            this.activeChunk = (c != null) ? c.getId() : "CH-000";
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(10, 14, 23));
            g2.fillRect(0, 0, w, h);

            if ("Waiting".equals(consStatus)) {
                g2.setColor(new Color(239, 68, 68));
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("⚠️ BUFFERING...", w / 2 - 50, h / 2);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("Waiting for server chunks (Buffer Empty)", w / 2 - 90, h / 2 + 20);
            } else if ("Running".equals(consStatus)) {
                g2.setColor(new Color(16, 185, 129));
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("▶ PLAYING STREAM", w / 2 - 65, h / 2);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(Color.CYAN);
                g2.drawString("Rendering: " + activeChunk, w / 2 - 45, h / 2 + 20);
            } else {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("⏹ IDLE / STOPPED", w / 2 - 55, h / 2);
            }
        }
    }

    // --- 4. Production vs Consumption ---
    private static class ProductionConsumptionCanvas extends JPanel {
        private List<Integer> prod = new ArrayList<>();
        private List<Integer> cons = new ArrayList<>();

        public void updateData(List<Integer> prod, List<Integer> cons) {
            this.prod = new ArrayList<>(prod);
            this.cons = new ArrayList<>(cons);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(10, 14, 23));
            g2.fillRect(0, 0, w, h);

            if (prod.size() < 2) return;

            int margin = 25;
            int chartW = w - 2 * margin;
            int chartH = h - 2 * margin;
            int maxVal = Math.max(1, Math.max(prod.get(prod.size() - 1), 10));

            // Produced Line (Purple)
            g2.setColor(new Color(139, 92, 246));
            g2.setStroke(new BasicStroke(2));
            for (int i = 0; i < prod.size() - 1; i++) {
                int x1 = margin + (i * chartW / 40);
                int y1 = h - margin - (prod.get(i) * chartH / maxVal);
                int x2 = margin + ((i + 1) * chartW / 40);
                int y2 = h - margin - (prod.get(i + 1) * chartH / maxVal);
                g2.drawLine(x1, y1, x2, y2);
            }

            // Consumed Line (Green)
            g2.setColor(new Color(16, 185, 129));
            for (int i = 0; i < cons.size() - 1; i++) {
                int x1 = margin + (i * chartW / 40);
                int y1 = h - margin - (cons.get(i) * chartH / maxVal);
                int x2 = margin + ((i + 1) * chartW / 40);
                int y2 = h - margin - (cons.get(i + 1) * chartH / maxVal);
                g2.drawLine(x1, y1, x2, y2);
            }

            // Legend
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.setColor(new Color(139, 92, 246));
            g2.drawString("Produced", margin, margin - 4);
            g2.setColor(new Color(16, 185, 129));
            g2.drawString("Consumed", margin + 60, margin - 4);
        }
    }
}