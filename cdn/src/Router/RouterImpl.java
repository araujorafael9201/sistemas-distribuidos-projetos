package Router;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import utils.Crypto;
import Origin.Origin;

public class RouterImpl implements Runnable {
    private SocketAddress from;
    private byte tag;
    private byte[] payload;

    public RouterImpl(DatagramPacket packet) {
        this.from = packet.getSocketAddress();
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
        this.tag = data.length > 0 ? data[0] : 0;
        this.payload = data.length > 0 ? Arrays.copyOfRange(data, 1, data.length) : new byte[0];
    }

    @Override
    public void run() {
        switch (tag) {
            case Router.HELLO:
                System.out.println("Handshake started by " + from + ", sending public key");
                Router.send(from, Router.KEY, Router.keys.getPublic().getEncoded());
                break;
            case Router.SESSION:
                handshake();
                break;
            case Router.DATA:
                request();
                break;
            default:
                System.out.println("Unknown packet from " + from + ", discarded");
        }
    }

    private void handshake() {
        try {
            Crypto.Session session = Crypto.unwrapKeys(Router.keys.getPrivate(), payload);
            Router.sessions.put(from, session);
            System.out.println("Session keys (AES + HMAC) and client RSA public key received from " + from);
            Router.send(from, Router.DATA, Crypto.seal(session, "READY"));
        } catch (GeneralSecurityException e) {
            System.out.println("Handshake decryption failed for " + from + ": " + e.getMessage());
        }
    }

    private void request() {
        Crypto.Session session = Router.sessions.get(from);
        if (session == null) {
            System.out.println("No session for " + from + ", packet discarded");
            return;
        }

        String request;
        try {
            request = Crypto.open(session, payload);
        } catch (GeneralSecurityException e) {
            System.out.println("Integrity failure (" + e.getMessage() + ") from " + from + ": packet discarded");
            return;
        }
        System.out.println("Received from " + from + " (HMAC + RSA signature valid): " + request);

        String response = handle(request);
        try {
            Router.send(from, Router.DATA, Crypto.seal(session, response));
            System.out.println("Sent to " + from + ": " + response);
        } catch (GeneralSecurityException e) {
            System.out.println("Could not encrypt reply to " + from + ": " + e.getMessage());
        }
    }

    // WHERE:<arquivo> -> OK:<host>:<porta do edge>
    private String handle(String request) {
        String[] parts = request.split(":", 2);
        if (parts.length != 2 || !parts[0].equals("WHERE")) {
            System.out.println("Unknown request: " + request);
            return "ERR:" + Origin.INPUT_ERROR;
        }
        String edge = Router.EDGE_HOST + ":" + Router.EDGE_PORT;
        System.out.println("Client asking for " + parts[1] + " -> edge " + edge);
        return "OK:" + edge;
    }
}
