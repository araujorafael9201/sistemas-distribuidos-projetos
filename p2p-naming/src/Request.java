import java.io.Serializable;

public class Request implements Serializable
{
    private String resource;
    private int ttl;

    public Request(String resource, int ttl)
    {
        this.resource = resource;
        this.ttl = ttl;
    }

    public String getResource() {
        return resource;
    }

    public int getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", resource, ttl);
    }

    public static Request fromString(String s) {
        String resource = s.split(":")[0];
        int ttl = Integer.parseInt(s.split(":")[1]);

        return new Request(resource, ttl);
    }
}
