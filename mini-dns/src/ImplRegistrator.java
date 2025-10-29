import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ImplRegistrator implements Runnable {
    private Scanner scanner;
    private Socket server;

    public ImplRegistrator(Socket server) {
        this.server = server;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        try {
            System.out.println("Enter host to register/update: ");
            String host = scanner.nextLine().trim();
            System.out.println("Enter address to register/update host " + host + ": ");
            String addr = scanner.nextLine().trim();

            DataOutputStream outputStream = new DataOutputStream(server.getOutputStream());
            DataInputStream inputStream = new DataInputStream(server.getInputStream());

            String msg = String.format("REGISTER:%s:%s", host, addr);
            System.out.println("Sending registration: " + msg + " to " + server.getRemoteSocketAddress());
            outputStream.writeUTF(msg);

            String resp = inputStream.readUTF();
            System.out.println("Server response: " + resp);

            outputStream.close();
            inputStream.close();
            server.close();
        } catch (IOException e) {
            System.err.println("Error updating address on server: " + e.getMessage());
        }
    }
}


