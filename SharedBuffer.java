import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedBuffer {
    private final LinkedList<Chunk> buffer = new LinkedList<>();
    private int capacity;

    private final AtomicInteger totalProduced = new AtomicInteger(0);
    private final AtomicInteger totalConsumed = new AtomicInteger(0);
    private final AtomicInteger producerWaitEvents = new AtomicInteger(0);
    private final AtomicInteger consumerWaitEvents = new AtomicInteger(0);

    // Performance Metrics Tracking
    private int maxOccupancy = 0;
    private double cumulativeOccupancyTime = 0;
    private long lastOccupancyChangeTime = System.currentTimeMillis();
    private final long startTimeMs = System.currentTimeMillis();

    private long totalProducerWaitTimeMs = 0;
    private long totalConsumerWaitTimeMs = 0;

    private final List<String> logs = new ArrayList<>();

    public SharedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void produce(Chunk chunk) throws InterruptedException {
        long waitStart = System.currentTimeMillis();
        while (buffer.size() >= capacity) {
            producerWaitEvents.incrementAndGet();
            addLog("PRODUCER WAITING (BUFFER FULL): " + buffer.size() + "/" + capacity);
            wait();
            totalProducerWaitTimeMs += (System.currentTimeMillis() - waitStart);
            waitStart = System.currentTimeMillis();
        }
        updateOccupancyMetrics();
        buffer.add(chunk);
        maxOccupancy = Math.max(maxOccupancy, buffer.size());
        totalProduced.incrementAndGet();
        addLog("PRODUCED: " + chunk.getId() + " -> Shared Buffer (" + buffer.size() + "/" + capacity + ")");
        notifyAll();
    }

    public synchronized Chunk consume() throws InterruptedException {
        long waitStart = System.currentTimeMillis();
        while (buffer.isEmpty()) {
            consumerWaitEvents.incrementAndGet();
            addLog("CONSUMER WAITING (BUFFER EMPTY): 0/" + capacity);
            wait();
            totalConsumerWaitTimeMs += (System.currentTimeMillis() - waitStart);
            waitStart = System.currentTimeMillis();
        }
        updateOccupancyMetrics();
        Chunk chunk = buffer.poll();
        totalConsumed.incrementAndGet();
        addLog("CONSUMED: " + chunk.getId() + " -> Media Player (" + buffer.size() + "/" + capacity + ")");
        notifyAll();
        return chunk;
    }

    private void updateOccupancyMetrics() {
        long now = System.currentTimeMillis();
        long duration = now - lastOccupancyChangeTime;
        cumulativeOccupancyTime += buffer.size() * (duration / 1000.0);
        lastOccupancyChangeTime = now;
    }

    public synchronized boolean setCapacity(int newCapacity) {
        if (newCapacity < buffer.size()) {
            addLog("RESIZE BLOCKED: Cannot reduce capacity to " + newCapacity + " while buffer holds " + buffer.size() + " chunks.");
            return false;
        }
        this.capacity = newCapacity;
        addLog("BUFFER CAPACITY CHANGED: New capacity = " + newCapacity + " slots.");
        notifyAll();
        return true;
    }

    private synchronized void addLog(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logs.add(0, "[" + time + "] " + message);
        if (logs.size() > 50) logs.remove(logs.size() - 1);
    }

    public synchronized int getCapacity() { return capacity; }
    public synchronized int getCurrentSize() { return buffer.size(); }
    public synchronized List<Chunk> getBufferContents() { return new ArrayList<>(buffer); }
    public int getTotalProduced() { return totalProduced.get(); }
    public int getTotalConsumed() { return totalConsumed.get(); }
    public int getProducerWaitEvents() { return producerWaitEvents.get(); }
    public int getConsumerWaitEvents() { return consumerWaitEvents.get(); }
    public synchronized List<String> getLogs() { return new ArrayList<>(logs); }

    // Metric Getters
    public synchronized int getMaxOccupancy() { return maxOccupancy; }
    public synchronized double getAverageOccupancy() {
        updateOccupancyMetrics();
        double totalRuntimeSec = Math.max(1.0, (System.currentTimeMillis() - startTimeMs) / 1000.0);
        return cumulativeOccupancyTime / totalRuntimeSec;
    }
    public synchronized double getProducerWaitDurationSec() { return totalProducerWaitTimeMs / 1000.0; }
    public synchronized double getConsumerWaitDurationSec() { return totalConsumerWaitTimeMs / 1000.0; }
    public synchronized double getSystemRuntimeSec() { return Math.max(1.0, (System.currentTimeMillis() - startTimeMs) / 1000.0); }
}