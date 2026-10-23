import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class MainDashboard extends JPanel {
    private final SharedBuffer buffer;
    private final ProducerTask producerTask;
    private final ConsumerTask consumerTask;

    private final JLabel lblCapacity = createMetricValue();
    private final JLabel lblCurrent = createMetricValue();
    private final JLabel lblUtilization = createMetricValue();
    private final JLabel lblProduced = createMetricValue();
    private final JLabel lblConsumed = createMetricValue();
    
    // Performance Metrics Labels
    private final JLabel lblProdRate = createMetricValue();
    private final JLabel lblConsRate = createMetricValue();
    private final JLabel lblMaxOccupancy = createMetricValue();
    private final JLabel lblAvgOccupancy = createMetricValue();
    private final JLabel lblProdWaitDuration = createMetricValue();
    private final JLabel lblConsWaitDuration = createMetricValue();
    private final JLabel lblBufferingEvents = createMetricValue();
    private final JLabel lblThroughput = createMetricValue();

    private final JProgressBar utilizationBar = new JProgressBar(0, 100);
    private final JLabel lblBufferStatusBanner = new JLabel("BUFFER NORMAL", SwingConstants.CENTER);

    private final JPanel bufferGridPanel = new JPanel(new GridLayout(2, 8, 8, 8));
    private final DefaultListModel<String> logListModel = new DefaultListModel<>();

    public MainDashboard(SharedBuffer buffer, ProducerTask producerTask, ConsumerTask consumerTask) {
        this.buffer = buffer;
        this.producerTask = producerTask;
        this.consumerTask = consumerTask;

        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(11, 15, 25));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Control Bar
        JPanel topContainer = new JPanel(new BorderLayout(10, 10));
        topContainer.setOpaque(false);

        JPanel capacityControlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        capacityControlBar.setBackground(new Color(18, 26, 43));
        capacityControlBar.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 20)));

        JLabel lblCapTitle = new JLabel("Buffer Capacity Selector:");
        lblCapTitle.setForeground(Color.WHITE);
        lblCapTitle.setFont(new Font("SansSerif", Font.BOLD, 12));

        JComboBox<Integer> capacityBox = new JComboBox<>(new Integer[]{4, 8, 10, 16});
        capacityBox.setSelectedItem(10);
        capacityBox.setFont(new Font("SansSerif", Font.BOLD, 12));

        capacityBox.addActionListener(e -> {
            int newCap = (Integer) capacityBox.getSelectedItem();
            boolean success = buffer.setCapacity(newCap);
            if (!success) {
                JOptionPane.showMessageDialog(this,
                        "Cannot reduce capacity to " + newCap + " slots because the buffer currently holds " + buffer.getCurrentSize() + " chunks.\nConsume chunks first before shrinking.",
                        "Capacity Change Blocked", JOptionPane.WARNING_MESSAGE);
                capacityBox.setSelectedItem(buffer.getCapacity());
            }
        });

        capacityControlBar.add(lblCapTitle);
        capacityControlBar.add(capacityBox);

        topContainer.add(capacityControlBar, BorderLayout.NORTH);

        // Performance Metrics Grid (3 rows x 4 cols)
        JPanel metricsPanel = new JPanel(new GridLayout(3, 4, 8, 8));
        metricsPanel.setOpaque(false);

        metricsPanel.add(createCard("Buffer Capacity", lblCapacity));
        metricsPanel.add(createCard("Chunks in Buffer", lblCurrent));
        metricsPanel.add(createCard("Buffer Utilization", lblUtilization));
        metricsPanel.add(createCard("Approx. Throughput", lblThroughput));

        metricsPanel.add(createCard("Avg Production Rate", lblProdRate));
        metricsPanel.add(createCard("Avg Consumption Rate", lblConsRate));
        metricsPanel.add(createCard("Max Buffer Occupancy", lblMaxOccupancy));
        metricsPanel.add(createCard("Avg Buffer Occupancy", lblAvgOccupancy));

        metricsPanel.add(createCard("Total Produced / Consumed", lblProduced));
        metricsPanel.add(createCard("Producer Wait Duration", lblProdWaitDuration));
        metricsPanel.add(createCard("Consumer Wait Duration", lblConsWaitDuration));
        metricsPanel.add(createCard("Buffering Underflows", lblBufferingEvents));

        topContainer.add(metricsPanel, BorderLayout.CENTER);

        // Shared Buffer Grid View
        JPanel bufferContainer = new JPanel(new BorderLayout(8, 8));
        bufferContainer.setBackground(new Color(18, 26, 43));
        bufferContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 20)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel barContainer = new JPanel(new BorderLayout(8, 0));
        barContainer.setOpaque(false);

        JLabel lblBarTitle = new JLabel("Utilization Gauge:");
        lblBarTitle.setForeground(Color.WHITE);
        lblBarTitle.setFont(new Font("SansSerif", Font.BOLD, 11));

        utilizationBar.setStringPainted(true);
        utilizationBar.setForeground(new Color(6, 182, 212));
        utilizationBar.setBackground(new Color(10, 14, 23));
        utilizationBar.setPreferredSize(new Dimension(0, 18));

        barContainer.add(lblBarTitle, BorderLayout.WEST);
        barContainer.add(utilizationBar, BorderLayout.CENTER);

        bufferContainer.add(barContainer, BorderLayout.NORTH);

        bufferGridPanel.setOpaque(false);
        bufferContainer.add(bufferGridPanel, BorderLayout.CENTER);

        // System Log
        JList<String> logList = new JList<>(logListModel);
        logList.setBackground(new Color(10, 14, 23));
        logList.setForeground(new Color(6, 182, 212));
        logList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollLog = new JScrollPane(logList);
        scrollLog.setPreferredSize(new Dimension(0, 130));
        scrollLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 30)),
                "System Event Log", 0, 0, new Font("SansSerif", Font.BOLD, 10), Color.GRAY
        ));

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(bufferContainer, BorderLayout.CENTER);
        centerPanel.add(scrollLog, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void updateUIState() {
        int capacity = buffer.getCapacity();
        int currentSize = buffer.getCurrentSize();
        int produced = buffer.getTotalProduced();
        int consumed = buffer.getTotalConsumed();
        double runtimeSec = buffer.getSystemRuntimeSec();

        lblCapacity.setText(capacity + " Slots");
        lblCurrent.setText(String.valueOf(currentSize));

        int util = (int) (((double) currentSize / capacity) * 100);
        lblUtilization.setText(util + "%");
        utilizationBar.setValue(util);
        utilizationBar.setString(String.format("%d / %d Chunks (%d%%)", currentSize, capacity, util));

        // Calculated Metric Updates
        lblProdRate.setText(String.format("%.2f /sec", produced / runtimeSec));
        lblConsRate.setText(String.format("%.2f /sec", consumed / runtimeSec));
        lblMaxOccupancy.setText(buffer.getMaxOccupancy() + " Slots");
        lblAvgOccupancy.setText(String.format("%.1f Chunks", buffer.getAverageOccupancy()));

        lblProduced.setText(produced + " P / " + consumed + " C");
        lblProdWaitDuration.setText(String.format("%.2f sec", buffer.getProducerWaitDurationSec()));
        lblConsWaitDuration.setText(String.format("%.2f sec", buffer.getConsumerWaitDurationSec()));
        lblBufferingEvents.setText(buffer.getConsumerWaitEvents() + " Events");
        lblThroughput.setText(String.format("%.2f Chunks/s", consumed / runtimeSec));

        // Buffer Slots Grid
        bufferGridPanel.removeAll();
        bufferGridPanel.setLayout(new GridLayout(capacity > 8 ? 2 : 1, Math.min(capacity, 8), 6, 6));

        List<Chunk> contents = buffer.getBufferContents();
        Chunk activeConsumingChunk = consumerTask.getCurrentChunk();

        for (int i = 0; i < capacity; i++) {
            JPanel slot = new JPanel(new BorderLayout(2, 2));
            slot.setPreferredSize(new Dimension(80, 50));

            if (i < contents.size()) {
                Chunk chunk = contents.get(i);
                boolean isBeingConsumed = (activeConsumingChunk != null && activeConsumingChunk.getId().equals(chunk.getId()) && consumerTask.isRunning() && !consumerTask.isPaused());

                if (isBeingConsumed) {
                    slot.setBackground(new Color(239, 68, 68, 120));
                    slot.setBorder(BorderFactory.createLineBorder(new Color(239, 68, 68), 2));
                } else {
                    slot.setBackground(new Color(139, 92, 246, 90));
                    slot.setBorder(BorderFactory.createLineBorder(new Color(6, 182, 212), 1));
                }

                JLabel lblId = new JLabel(chunk.getId(), SwingConstants.CENTER);
                lblId.setFont(new Font("SansSerif", Font.BOLD, 11));
                lblId.setForeground(Color.CYAN);

                JLabel lblDetails = new JLabel(String.format("<html><center>#%d | %.1fMB</center></html>", (i + 1), chunk.getSizeMb()), SwingConstants.CENTER);
                lblDetails.setFont(new Font("SansSerif", Font.PLAIN, 8));
                lblDetails.setForeground(Color.LIGHT_GRAY);

                slot.add(lblId, BorderLayout.CENTER);
                slot.add(lblDetails, BorderLayout.SOUTH);
            } else {
                slot.setBackground(new Color(0, 0, 0, 90));
                slot.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 20), 1));

                JLabel lblEmpty = new JLabel("EMPTY", SwingConstants.CENTER);
                lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 9));
                lblEmpty.setForeground(Color.GRAY);

                slot.add(lblEmpty, BorderLayout.CENTER);
            }
            bufferGridPanel.add(slot);
        }

        logListModel.clear();
        for (String log : buffer.getLogs()) {
            logListModel.addElement(log);
        }

        revalidate();
        repaint();
    }

    private JPanel createCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(new Color(18, 26, 43));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 20)),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 9));
        lblTitle.setForeground(new Color(156, 163, 175));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JLabel createMetricValue() {
        JLabel label = new JLabel("0");
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(Color.WHITE);
        return label;
    }
}