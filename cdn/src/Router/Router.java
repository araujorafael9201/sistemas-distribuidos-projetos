package Router;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import utils.Crypto;

public class Router {
    public static final byte HELLO = 'H';
    public static final byte KEY = 'K';
    public static final byte SESSION = 'S';
    public static final byte DATA = 'D';

    // Localização fictícia do cliente -> porta do edge que atende essa região
    public static final Map<String, Integer> edges = Map.of(
            "NORTE", 8001,
            "SUL", 8002,
            "LESTE", 8003,
            "OESTE", 8004);

    // Região atendida pelo edge nessa porta, ou null
    public static String region(int port) {
        for (Map.Entry<String, Integer> e : edges.entrySet()) {
            if (e.getValue() == port) {
                return e.getKey();
            }
        }
        return null;
    }

    // Rótulo do edge para os logs, ex.: "SUL (8002)"
    public static String edgeLabel(int port) {
        String region = region(port);
        return region == null ? String.valueOf(port) : region + " (" + port + ")";
    }

    static DatagramSocket socket;
    static KeyPair keys;
    static ConcurrentHashMap<SocketAddress, Crypto.Session> sessions = new ConcurrentHashMap<>();

    private int port;

    public Router(int port) {
        this.port = port;
        run();
    }

    private void run() {
        try {
            keys = Crypto.newKeyPair();
        } catch (GeneralSecurityException e) {
            System.err.println("Could not generate RSA keys: " + e.getMessage());
            return;
        }

        try (DatagramSocket s = new DatagramSocket(port)) {
            socket = s;
            System.out.println("Router listening on UDP port " + port);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
                socket.receive(packet);
                new Thread(new RouterImpl(packet)).start();
            }
        } catch (IOException e) {
            System.err.println("Router error: " + e.getMessage());
        }
    }

    static void send(SocketAddress to, byte tag, byte[] payload) {
        byte[] data = Crypto.concat(new byte[]{tag}, payload);
        try {
            socket.send(new DatagramPacket(data, data.length, to));
        } catch (IOException e) {
            System.out.println("Could not send to " + to + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 8081;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Router.Router [port]");
                return;
            }
        }
        new Router(port);
    }
}
