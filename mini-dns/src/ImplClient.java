import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ImplClient implements Runnable {
    private Socket server;

    public ImplClient(Socket server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            String address = null;
            DataOutputStream outputStream = new DataOutputStream(server.getOutputStream());
            DataInputStream inputStream = new DataInputStream(server.getInputStream());
            
            outputStream.writeUTF("servidor9");
            address = inputStream.readUTF();
            System.out.println("servidor9" + " address is " + address);

            outputStream.close();
            inputStream.close();
            server.close();
        } catch (IOException e) {
            System.err.println("Error fetching host address from server: " + e.getMessage());
        }
    }
}
