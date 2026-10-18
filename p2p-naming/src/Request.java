// Mensagem de busca que circula pelo anel: "<arquivo>:<origem>:<ttl>".
public class Request {
    private String resource;
    private int origin;
    private int ttl;

    public Request(String resource, int origin, int ttl) {
        this.resource = resource;
        this.origin = origin;
        this.ttl = ttl;
    }

    public String getResource() {
        return resource;
    }

    public int getOrigin() {
        return origin;
    }

    public int getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        return resource + ":" + origin + ":" + ttl;
    }

    public static Request fromString(String s) {
        String[] parts = s.split(":");
        return new Request(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
