import java.io.IOException;
import java.net.DatagramSocket;
import java.security.GeneralSecurityException;
import java.util.Map;

// Receives every reply and update pushed by the DNS server.
public class ImplClient implements Runnable {
    private DatagramSocket socket;
    private Crypto.Session session;
    private Map<String, String> table;

    public ImplClient(DatagramSocket socket, Crypto.Session session, Map<String, String> table) {
        this.socket = socket;
        this.session = session;
        this.table = table;
    }

    @Override
    public void run() {
        while (!socket.isClosed()) {
            String message;
            try {
                message = Crypto.open(session, Client.receive(socket, Server.DATA));
            } catch (GeneralSecurityException e) {
                System.out.println("Invalid message (HMAC) discarded: " + e.getMessage());
                continue;
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.out.println("Receive error: " + e.getMessage());
                }
                continue;
            }
            System.out.println("Received (HMAC valid): " + message);

            String[] parts = message.split(":", 3);
            if (parts[0].equals("OK") && parts.length == 3) {
                table.put(parts[1], parts[2]);
                System.out.println(parts[1] + " address is " + parts[2]);
            } else if (parts[0].equals("UPDATE") && parts.length == 3) {
                table.put(parts[1], parts[2]);
                System.out.println("Update received from DNS: " + parts[1] + " -> " + parts[2]);
            } else if (parts[0].equals("ERR")) {
                System.out.println(message.substring("ERR:".length()));
            }
        }
    }
}
