import javax.swing.*;
import java.awt.*;

public class StreamApp extends JFrame {
    private final SharedBuffer sharedBuffer = new SharedBuffer(10);
    private final ProducerTask producerTask = new ProducerTask(sharedBuffer);
    private final ConsumerTask consumerTask = new ConsumerTask(sharedBuffer);

    private final MainDashboard mainDashboard = new MainDashboard(sharedBuffer, producerTask, consumerTask);
    private final ProducerPanel producerPanel = new ProducerPanel(producerTask, sharedBuffer);
    private final ConsumerPanel consumerPanel = new ConsumerPanel(consumerTask, sharedBuffer);
    private final VisualizationsPanel visualizationsPanel = new VisualizationsPanel(sharedBuffer, producerTask, consumerTask);

    public StreamApp() {
        setTitle("Producer-Consumer OS Synchronization Dashboard");
        setSize(1200, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        new Thread(producerTask, "Producer-Thread").start();
        new Thread(consumerTask, "Consumer-Thread").start();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabbedPane.setBackground(new Color(18, 26, 43));
        tabbedPane.setForeground(Color.WHITE);

        tabbedPane.addTab("Main Overview", mainDashboard);
        tabbedPane.addTab("Producer Server", producerPanel);
        tabbedPane.addTab("Consumer Client", consumerPanel);
        tabbedPane.addTab("Real-Time Visualizations", visualizationsPanel); // NEW TAB

        add(tabbedPane);

        Timer uiTimer = new Timer(100, e -> {
            mainDashboard.updateUIState();
            producerPanel.updateUIState();
            consumerPanel.updateUIState();
            visualizationsPanel.updateUIState();
        });
        uiTimer.start();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new StreamApp().setVisible(true));
    }
}