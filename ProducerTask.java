import java.util.ArrayList;
import java.util.List;

public class ProducerTask implements Runnable {
    private final SharedBuffer sharedBuffer;
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private volatile boolean isBlocked = false;
    private volatile int intervalMs = 1500;

    private int chunkSequence = 1;
    private volatile Chunk currentChunk = null;
    private volatile long lastChunkProductionTime = 0;

    private final List<ProductionRecord> productionHistory = new ArrayList<>();

    public static class ProductionRecord {
        public final String chunkId;
        public final String size;
        public final long timeTakenMs;
        public final String timestamp;

        public ProductionRecord(String chunkId, String size, long timeTakenMs, String timestamp) {
            this.chunkId = chunkId;
            this.size = size;
            this.timeTakenMs = timeTakenMs;
            this.timestamp = timestamp;
        }
    }

    public ProducerTask(SharedBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
    }

    public void start() { running = true; paused = false; isBlocked = false; }
    public void pause() { paused = true; isBlocked = false; }
    public void resume() { paused = false; isBlocked = false; }
    public void stop() { running = false; paused = false; isBlocked = false; currentChunk = null; }
    public void setIntervalMs(int intervalMs) { this.intervalMs = intervalMs; }

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public boolean isBlocked() { return isBlocked; }
    public int getIntervalMs() { return intervalMs; }
    public Chunk getCurrentChunk() { return currentChunk; }
    public long getLastChunkProductionTime() { return lastChunkProductionTime; }
    public synchronized List<ProductionRecord> getProductionHistory() { return new ArrayList<>(productionHistory); }

    public String getStatusString() {
        if (!running) return "Stopped";
        if (paused) return "Paused";
        if (isBlocked) return "Waiting";
        return "Running";
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (running && !paused) {
                    long startTime = System.currentTimeMillis();
                    Chunk chunk = new Chunk(chunkSequence);
                    currentChunk = chunk;

                    Thread.sleep(Math.max(100, intervalMs / 2));
                    long timeTaken = System.currentTimeMillis() - startTime;

                    isBlocked = (sharedBuffer.getCurrentSize() >= sharedBuffer.getCapacity());
                    sharedBuffer.produce(chunk);
                    isBlocked = false;

                    lastChunkProductionTime = timeTaken;
                    chunkSequence++;

                    synchronized (this) {
                        productionHistory.add(0, new ProductionRecord(
                                chunk.getId(),
                                String.format("%.1f MB", chunk.getSizeMb()),
                                timeTaken,
                                chunk.getTimestamp()
                        ));
                        if (productionHistory.size() > 25) productionHistory.remove(productionHistory.size() - 1);
                    }
                } else {
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}