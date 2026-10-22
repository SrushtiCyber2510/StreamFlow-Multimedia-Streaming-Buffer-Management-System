public interface StreamListener {

    void log(String message);

    void updateStats();

    void productionCompleted();

    void deviceCompleted(String deviceName);
}