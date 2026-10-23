import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Chunk {
    private final String id;
    private final double sizeMb;
    private final String timestamp;

    public Chunk(int idNumber) {
        this.id = String.format("CH-%03d", idNumber);
        this.sizeMb = 1.5 + (Math.random() * 2.0);
        this.timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getId() { return id; }
    public double getSizeMb() { return sizeMb; }
    public String getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("%s (%.1f MB)", id, sizeMb);
    }
}