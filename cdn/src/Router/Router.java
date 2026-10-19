package Router;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.ConcurrentHashMap;

import utils.Crypto;

public class Router {
    public static final byte HELLO = 'H';
    public static final byte KEY = 'K';
    public static final byte SESSION = 'S';
    public static final byte DATA = 'D';

    // Edge que atende todos os clientes
    public static final String EDGE_HOST = "127.0.0.1";
    public static final int EDGE_PORT = 8001;

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
