import java.util.ArrayList;
import java.util.List;

public class ConsumerTask implements Runnable {
    private final SharedBuffer sharedBuffer;
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private volatile boolean isWaiting = false;
    private volatile int intervalMs = 2000;

    private volatile Chunk currentChunk = null;
    private volatile long lastChunkConsumeTime = 0;

    private final List<PlaybackRecord> playbackHistory = new ArrayList<>();

    public static class PlaybackRecord {
        public final String chunkId;
        public final String size;
        public final long renderTimeMs;
        public final String timestamp;

        public PlaybackRecord(String chunkId, String size, long renderTimeMs, String timestamp) {
            this.chunkId = chunkId;
            this.size = size;
            this.renderTimeMs = renderTimeMs;
            this.timestamp = timestamp;
        }
    }

    public ConsumerTask(SharedBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
    }

    public void start() { running = true; paused = false; isWaiting = false; }
    public void pause() { paused = true; isWaiting = false; }
    public void resume() { paused = false; isWaiting = false; }
    public void stop() { running = false; paused = false; isWaiting = false; currentChunk = null; }
    public void setIntervalMs(int intervalMs) { this.intervalMs = intervalMs; }

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public boolean isWaiting() { return isWaiting; }
    public int getIntervalMs() { return intervalMs; }
    public Chunk getCurrentChunk() { return currentChunk; }
    public long getLastChunkConsumeTime() { return lastChunkConsumeTime; }
    public synchronized List<PlaybackRecord> getPlaybackHistory() { return new ArrayList<>(playbackHistory); }

    public String getStatusString() {
        if (!running) return "Stopped";
        if (paused) return "Paused";
        if (isWaiting) return "Waiting";
        return "Running";
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (running && !paused) {
                    long startTime = System.currentTimeMillis();
                    isWaiting = (sharedBuffer.getCurrentSize() == 0);

                    Chunk chunk = sharedBuffer.consume();
                    isWaiting = false;

                    if (chunk != null) {
                        currentChunk = chunk;
                        Thread.sleep(Math.max(100, intervalMs / 2));
                        long timeTaken = System.currentTimeMillis() - startTime;
                        lastChunkConsumeTime = timeTaken;

                        synchronized (this) {
                            playbackHistory.add(0, new PlaybackRecord(
                                    chunk.getId(),
                                    String.format("%.1f MB", chunk.getSizeMb()),
                                    timeTaken,
                                    chunk.getTimestamp()
                            ));
                            if (playbackHistory.size() > 25) playbackHistory.remove(playbackHistory.size() - 1);
                        }
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