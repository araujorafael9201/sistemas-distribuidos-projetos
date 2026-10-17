import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class ImplServer implements Runnable {
    private SocketAddress from;
    private byte tag;
    private byte[] payload;

    public ImplServer(DatagramPacket packet) {
        this.from = packet.getSocketAddress();
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
        this.tag = data.length > 0 ? data[0] : 0;
        this.payload = data.length > 0 ? Arrays.copyOfRange(data, 1, data.length) : new byte[0];
    }

    @Override
    public void run() {
        switch (tag) {
            case Server.HELLO:
                System.out.println("Handshake started by " + from + ", sending public key");
                Server.send(from, Server.KEY, Server.keys.getPublic().getEncoded());
                break;
            case Server.SESSION:
                handshake();
                break;
            case Server.DATA:
                request();
                break;
            default:
                System.out.println("Unknown packet from " + from + ", discarded");
        }
    }

    private void handshake() {
        try {
            Crypto.Session session = Crypto.unwrapKeys(Server.keys.getPrivate(), payload);
            Server.sessions.put(from, session);
            System.out.println("Session keys (AES + HMAC) received from " + from);
            Server.send(from, Server.DATA, Crypto.seal(session, "READY"));
        } catch (GeneralSecurityException e) {
            System.out.println("Handshake decryption failed for " + from + ": " + e.getMessage());
        }
    }

    private void request() {
        Crypto.Session session = Server.sessions.get(from);
        if (session == null) {
            System.out.println("No session for " + from + ", packet discarded");
            return;
        }

        String request;
        try {
            request = Crypto.open(session, payload);
        } catch (GeneralSecurityException e) {
            System.out.println("Integrity failure (HMAC) from " + from + ": packet discarded");
            return;
        }
        System.out.println("Received from " + from + " (HMAC valid): " + request);

        String response = handle(request);
        reply(from, session, response);
    }

    private String handle(String request) {
        if (request.equals("SUBSCRIBE")) {
            Server.subscribers.add(from);
            System.out.println(from + " subscribed to updates");
            return "OK";
        }

        if (request.startsWith("LOOKUP:")) {
            String name = request.substring("LOOKUP:".length());
            String address = Server.addresses.get(name);
            if (address == null) {
                System.out.println("Lookup failed: " + name);
                return "ERR:" + Server.INPUT_ERROR;
            }
            return "OK:" + name + ":" + address;
        }

        if (request.startsWith("REGISTER:")) {
            String[] parts = request.split(":", 3);
            if (parts.length != 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
                System.out.println("Invalid REGISTER: " + request);
                return "ERR:" + Server.INPUT_ERROR;
            }
            Server.addresses.put(parts[1], parts[2]);
            System.out.println("Registered " + parts[1] + " -> " + parts[2]);
            notifySubscribers("UPDATE:" + parts[1] + ":" + parts[2]);
            return "OK";
        }

        System.out.println("Unknown request: " + request);
        return "ERR:" + Server.INPUT_ERROR;
    }

    // Each subscriber gets the update encrypted with its own session keys.
    private void notifySubscribers(String update) {
        for (SocketAddress subscriber : Server.subscribers) {
            Crypto.Session session = Server.sessions.get(subscriber);
            if (session != null) {
                reply(subscriber, session, update);
            }
        }
    }

    private void reply(SocketAddress to, Crypto.Session session, String text) {
        try {
            Server.send(to, Server.DATA, Crypto.seal(session, text));
            System.out.println("Sent to " + to + ": " + text);
        } catch (GeneralSecurityException e) {
            System.out.println("Could not encrypt reply to " + to + ": " + e.getMessage());
        }
    }
}
