import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class ImplServer implements Runnable {
    private Socket client;
    public static HashMap<String, String> addresses = new HashMap<String, String>(){{
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

    public ImplServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            System.out.println("Connection established with " + client.getInetAddress().getHostAddress());  
            DataInputStream inputStream = new DataInputStream(client.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
            
            String requestHost = inputStream.readUTF();
            System.out.println("Client request: " + requestHost);
            String address = addresses.get(requestHost);
            if (address == null) {
                System.out.println("Host not found: " + requestHost + "; returning empty string");
                address = "";
            }
            outputStream.writeUTF(address);
            outputStream.flush();
            System.out.println("Successfully returned " + address + " to client");

            inputStream.close();
            outputStream.close();
            client.close();
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
