import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;

public class Registrator {
    private SocketAddress server;

    public Registrator(String serverAddr, int port) {
        this.server = new InetSocketAddress(serverAddr, port);
        run();
    }

    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            Crypto.Session session = Client.handshake(socket, server);
            Thread t = new Thread(new ImplRegistrator(socket, server, session));
            t.start();
            t.join();
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("Registrator error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Registrator interrupted");
        }
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = 8888;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Registrator [host] [port]");
                return;
            }
        }
        new Registrator(host, port);
    }
}
