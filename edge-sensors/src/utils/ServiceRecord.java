package utils;

import java.io.Serializable;

public class ServiceRecord implements Serializable {
    private String host;
    private int port;
    private String type; // "UDP", "RMI"

    public ServiceRecord(String host, int port, String type) {
        this.host = host;
        this.port = port;
        this.type = type;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getType() { return type; }
    
    @Override
    public String toString() {
        return type + "://" + host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRecord that = (ServiceRecord) o;
        return port == that.port &&
               host.equals(that.host) &&
               type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(host, port, type);
    }
}
