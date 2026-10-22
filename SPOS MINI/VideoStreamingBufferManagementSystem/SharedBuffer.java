import java.util.LinkedList;
import java.util.Queue;

public class SharedBuffer {

    private final Queue<MediaChunk> buffer = new LinkedList<>();
    private final int capacity;
    private final StreamListener listener;

    private int producedCount = 0;
    private int consumedCount = 0;
    private int producerWaits = 0;
    private int consumerWaits = 0;

    public SharedBuffer(int capacity, StreamListener listener) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Buffer capacity must be greater than 0."
            );
        }

        this.capacity = capacity;
        this.listener = listener;
    }

    // PRODUCER
    public synchronized void produce(MediaChunk chunk)
            throws InterruptedException {

        while (buffer.size() == capacity) {

            producerWaits++;

            if (listener != null) {
                listener.log(
                        "BUFFER FULL -> Producer is waiting..."
                );

                listener.updateStats();
            }

            // Producer waits when buffer is full
            wait();
        }

        buffer.add(chunk);

        // Do not count End-of-Stream as a media chunk
        if (!chunk.isEndOfStream()) {
            producedCount++;
        }

        // Wake up waiting consumers
        notifyAll();

        if (listener != null) {
            listener.updateStats();
        }
    }

    // CONSUMER
    public synchronized MediaChunk consume()
            throws InterruptedException {

        while (buffer.isEmpty()) {

            consumerWaits++;

            if (listener != null) {
                listener.log(
                        "BUFFER EMPTY -> Consumer is waiting..."
                );

                listener.updateStats();
            }

            // Consumer waits when buffer is empty
            wait();
        }

        MediaChunk chunk = buffer.remove();

        // Do not count End-of-Stream as a media chunk
        if (!chunk.isEndOfStream()) {
            consumedCount++;
        }

        // Wake up waiting producer
        notifyAll();

        if (listener != null) {
            listener.updateStats();
        }

        return chunk;
    }

    public synchronized int getBufferSize() {
        return buffer.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized int getProducedCount() {
        return producedCount;
    }

    public synchronized int getConsumedCount() {
        return consumedCount;
    }

    public synchronized int getProducerWaits() {
        return producerWaits;
    }

    public synchronized int getConsumerWaits() {
        return consumerWaits;
    }
}