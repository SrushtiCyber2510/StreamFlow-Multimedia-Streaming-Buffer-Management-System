public class UserDevice extends Thread {

    private final String deviceName;
    private final SharedBuffer buffer;
    private final int playbackDelay;
    private final StreamListener listener;

    private volatile boolean running = true;

    public UserDevice(
            String deviceName,
            SharedBuffer buffer,
            int playbackDelay,
            StreamListener listener) {

        this.deviceName = deviceName;
        this.buffer = buffer;
        this.playbackDelay = playbackDelay;
        this.listener = listener;
    }

    @Override
    public void run() {

        try {

            while (running) {

                MediaChunk chunk =
                        buffer.consume();

                if (chunk.isEndOfStream()) {

                    listener.log(
                            deviceName
                            + " -> Streaming completed."
                    );

                    break;
                }

                listener.log(
                        deviceName
                        + " -> Playing "
                        + chunk.getMediaType()
                        + " Chunk "
                        + chunk.getChunkNumber()
                );

                listener.updateStats();

                Thread.sleep(playbackDelay);
            }

        } catch (InterruptedException e) {

            if (running) {

                listener.log(
                        deviceName
                        + " -> Interrupted."
                );
            }

        } finally {

            running = false;

            listener.deviceCompleted(
                    deviceName
            );
        }
    }

    public void stopDevice() {

        running = false;

        interrupt();
    }
}