package Edge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.ConcurrentHashMap;

import utils.Crypto;
import Client.Client;

public class Edge {
    public static final long TTL = 10000;

    static int port;
    static KeyPair keys;
    // nome do arquivo -> instante em que foi salvo no cache
    static ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    private String originAddr;
    private int originPort;

    public Edge(int port, String originAddr, int originPort) {
        Edge.port = port;
        this.originAddr = originAddr;
        this.originPort = originPort;
        run();
    }

    static void log(String message) {
        System.out.println("[Edge " + port + "] " + message);
    }

    private void run() {
        try {
            keys = Crypto.newKeyPair();
        } catch (GeneralSecurityException e) {
            System.err.println("Could not generate RSA keys: " + e.getMessage());
            return;
        }

        try (ServerSocket ss = new ServerSocket(port)) {
            log("Listening on TCP port " + port + ", cache TTL " + (TTL / 1000) + " s");
            while (true) {
                Socket s = ss.accept();
                new Thread(new EdgeImpl(s, originAddr, originPort)).start();
            }
        } catch (IOException e) {
            System.err.println("Edge error: " + e.getMessage());
        }
    }

    // Diz se o arquivo está no cache e dentro do TTL; se expirou, remove.
    static boolean cached(String file) {
        Long savedAt = cache.get(file);
        if (savedAt == null) {
            return false;
        }
        long age = System.currentTimeMillis() - savedAt;
        if (age > TTL) {
            cache.remove(file);
            log(file + " expired (TTL, " + age + " ms old), removed from cache");
            return false;
        }
        return true;
    }

    // Baixa o arquivo da Origem por um canal cifrado. Devolve a resposta da Origem.
    static String fetch(String originAddr, int originPort, String file) throws IOException, GeneralSecurityException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(originAddr, originPort), 2000);
            socket.setSoTimeout(5000);
            Crypto.Session session = Client.handshake(socket, keys);

            String msg = "GET /" + file;
            Client.send(socket, Crypto.seal(session, msg));
            log("Sent to origin: " + msg);

            String resp = Crypto.open(session, Client.receive(socket));
            log("Origin response (HMAC + RSA signature valid): " + resp.replace("\n", " | "));
            return resp;
        }
    }

    public static void main(String[] args) {
        int port = 8001;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Edge.Edge [port]");
                return;
            }
        }
        new Edge(port, "127.0.0.1", 8000);
    }
}
