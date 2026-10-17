import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.util.Scanner;

public class ImplRegistrator implements Runnable {
    private Scanner scanner;
    private DatagramSocket socket;
    private SocketAddress server;
    private Crypto.Session session;

    public ImplRegistrator(DatagramSocket socket, SocketAddress server, Crypto.Session session) {
        this.socket = socket;
        this.server = server;
        this.session = session;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        try {
            System.out.println("Enter host to register/update: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String host = scanner.nextLine().trim();
            System.out.println("Enter address to register/update host " + host + ": ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String addr = scanner.nextLine().trim();

            String msg = String.format("REGISTER:%s:%s", host, addr);
            System.out.println("Sending registration: " + msg + " to " + server);
            Client.send(socket, server, Server.DATA, Crypto.seal(session, msg));

            socket.setSoTimeout(3000);
            String resp = Crypto.open(session, Client.receive(socket, Server.DATA));
            System.out.println("Server response (HMAC valid): " + resp);
            if (resp.startsWith("ERR:")) {
                System.out.println(resp.substring("ERR:".length()));
            }
        } catch (SocketTimeoutException e) {
            System.out.println("No response from server");
        } catch (GeneralSecurityException e) {
            System.out.println("Invalid response (HMAC) discarded: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error updating address on server: " + e.getMessage());
        }
    }
}
