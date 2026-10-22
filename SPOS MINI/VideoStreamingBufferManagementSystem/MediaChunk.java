public class MediaChunk {

    private final int chunkNumber;
    private final String mediaType;
    private final String data;
    private final boolean endOfStream;

    // Normal media chunk
    public MediaChunk(int chunkNumber, String mediaType) {
        this.chunkNumber = chunkNumber;
        this.mediaType = mediaType;
        this.data = mediaType + " Chunk " + chunkNumber;
        this.endOfStream = false;
    }

    // End-of-stream chunk
    private MediaChunk(boolean endOfStream) {
        this.chunkNumber = -1;
        this.mediaType = "";
        this.data = "END OF STREAM";
        this.endOfStream = endOfStream;
    }

    public static MediaChunk endOfStream() {
        return new MediaChunk(true);
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getData() {
        return data;
    }

    public boolean isEndOfStream() {
        return endOfStream;
    }
}