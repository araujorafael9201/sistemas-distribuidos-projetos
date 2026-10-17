import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    private SocketAddress server;
    private Map<String, String> table = new ConcurrentHashMap<>();

    public Client(String serverAddr, int port) {
        this.server = new InetSocketAddress(serverAddr, port);
        run();
    }

    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            Crypto.Session session = handshake(socket, server);

            Thread t = new Thread(new ImplClient(socket, session, table));
            t.setDaemon(true);
            t.start();

            send(socket, server, Server.DATA, Crypto.seal(session, "SUBSCRIBE"));
            System.out.println("Sent: SUBSCRIBE");

            menu(socket, session);
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private void menu(DatagramSocket socket, Crypto.Session session) throws IOException, GeneralSecurityException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Commands: <name> | corrupt <name> | tabela | sair");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("sair")) {
                break;
            }
            if (line.equals("tabela")) {
                System.out.println("Local table: " + table);
            } else if (line.startsWith("corrupt ")) {
                String request = "LOOKUP:" + line.substring("corrupt ".length()).trim();
                send(socket, server, Server.DATA, Crypto.corrupt(Crypto.seal(session, request)));
                System.out.println("Sent tampered packet: " + request + " (the server should discard it)");
            } else {
                send(socket, server, Server.DATA, Crypto.seal(session, "LOOKUP:" + line));
                System.out.println("Sent: LOOKUP:" + line);
            }
        }
    }

    // RSA handshake
    public static Crypto.Session handshake(DatagramSocket socket, SocketAddress server)
            throws IOException, GeneralSecurityException {
        socket.setSoTimeout(3000);
        try {
            send(socket, server, Server.HELLO, new byte[0]);
            byte[] publicKey = receive(socket, Server.KEY);

            Crypto.Session session = Crypto.newSession();
            send(socket, server, Server.SESSION, Crypto.wrapKeys(publicKey, session));

            String ready = Crypto.open(session, receive(socket, Server.DATA));
            if (!ready.equals("READY")) {
                throw new GeneralSecurityException("unexpected handshake reply: " + ready);
            }
            System.out.println("Secure channel established with " + server + " (HMAC valid)");
            return session;
        } finally {
            socket.setSoTimeout(0);
        }
    }

    static void send(DatagramSocket socket, SocketAddress to, byte tag, byte[] payload) throws IOException {
        byte[] data = Crypto.concat(new byte[]{tag}, payload);
        socket.send(new DatagramPacket(data, data.length, to));
    }

    static byte[] receive(DatagramSocket socket, byte tag) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
        socket.receive(packet);
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
        if (data.length == 0 || data[0] != tag) {
            throw new IOException("unexpected packet from server");
        }
        return Arrays.copyOfRange(data, 1, data.length);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = 8888;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Client [host] [port]");
                return;
            }
        }
        new Client(host, port);
    }
}
