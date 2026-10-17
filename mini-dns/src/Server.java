import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static final String INPUT_ERROR = "Erro na entrada de dados. Tente outra vez!";

    public static final byte HELLO = 'H';
    public static final byte KEY = 'K';
    public static final byte SESSION = 'S';
    public static final byte DATA = 'D';

    public static ConcurrentHashMap<String, String> addresses = new ConcurrentHashMap<String, String>(){{
        put("servidor1", "192.168.0.10");
        put("servidor2", "192.168.0.20");
        put("servidor3", "192.168.0.30");
        put("servidor4", "192.168.0.40");
        put("servidor5", "192.168.0.50");
        put("servidor6", "192.168.0.60");
        put("servidor7", "192.168.0.70");
        put("servidor8", "192.168.0.80");
        put("servidor9", "192.168.0.90");
        put("servidor10","192.168.0.100");
    }};

    static DatagramSocket socket;
    static KeyPair keys;
    static ConcurrentHashMap<SocketAddress, Crypto.Session> sessions = new ConcurrentHashMap<>();
    static Set<SocketAddress> subscribers = ConcurrentHashMap.newKeySet();

    private int port;

    public Server(int port) {
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
            System.out.println("Listening on UDP port " + port);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
                socket.receive(packet);
                new Thread(new ImplServer(packet)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
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
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Server [port]");
                return;
            }
        }
        new Server(port);
    }
}
