public class StreamingServer extends Thread {

    private final SharedBuffer buffer;
    private final int totalChunks;
    private final int productionDelay;
    private final String mediaType;
    private final int consumerCount;
    private final StreamListener listener;

    private volatile boolean running = true;

    public StreamingServer(
            SharedBuffer buffer,
            int totalChunks,
            int productionDelay,
            String mediaType,
            int consumerCount,
            StreamListener listener) {

        this.buffer = buffer;
        this.totalChunks = totalChunks;
        this.productionDelay = productionDelay;
        this.mediaType = mediaType;
        this.consumerCount = consumerCount;
        this.listener = listener;
    }

    public void stopServer() {

        running = false;
        interrupt();
    }

    @Override
    public void run() {

        try {

            // Produce media chunks
            for (int i = 1; i <= totalChunks && running; i++) {

                MediaChunk chunk =
                        new MediaChunk(i, mediaType);

                buffer.produce(chunk);

                listener.log(
                        "SERVER -> Produced "
                                + chunk.getData()
                );

                listener.updateStats();

                Thread.sleep(productionDelay);
            }

            if (running) {

                /*
                 * Send one end-of-stream signal
                 * to each consumer device.
                 */
                for (int i = 0; i < consumerCount; i++) {

                    buffer.produce(
                            MediaChunk.endOfStream()
                    );
                }

                listener.log(
                        "SERVER -> All media chunks produced."
                );

                listener.log(
                        "SERVER -> End-of-stream signals sent."
                );

                listener.updateStats();

                listener.productionCompleted();

            } else {

                listener.log(
                        "SERVER -> Stopped by user."
                );
            }

        } catch (InterruptedException e) {

            if (running) {

                listener.log(
                        "SERVER -> Interrupted."
                );
            }
        }
    }
}