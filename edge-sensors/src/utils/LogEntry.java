package utils;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private long index;
    private String operation;
    private SensorDTO data;
    private long timestamp;

    public LogEntry(long index, String operation, SensorDTO data) {
        this.index = index;
        this.operation = operation;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public long getIndex() { return index; }
    public String getOperation() { return operation; }
    public SensorDTO getData() { return data; }
    public long getTimestamp() { return timestamp; }
}
