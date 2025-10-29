import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ImplClient implements Runnable {
    private Scanner scanner;
    private Socket server;

    public ImplClient(Socket server) {
        this.server = server;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        try {
            System.out.print("Enter host to lookup: ");
            String host = scanner.nextLine().trim();

            DataOutputStream outputStream = new DataOutputStream(server.getOutputStream());
            DataInputStream inputStream = new DataInputStream(server.getInputStream());

            outputStream.writeUTF(host);

            String address = inputStream.readUTF();

            if (address != null && !address.isEmpty()) {
                System.out.println(host + " address is " + address);
            } else {
                System.out.println("Address not found for " + host);
            }

            outputStream.close();
            inputStream.close();
            server.close();
        } catch (IOException e) {
            System.err.println("Error fetching host address from server: " + e.getMessage());
        }
    }
}
