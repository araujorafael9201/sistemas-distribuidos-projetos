import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ImplServer implements Runnable {
    private Socket client;
    private ConcurrentHashMap<String, String> addresses;

    public ImplServer(Socket client, ConcurrentHashMap<String, String> addresses) {
        this.client = client;
        this.addresses = addresses;
    }

    @Override
    public void run() {
        try {
            System.out.println("Connection established with " + client.getInetAddress().getHostAddress());
            DataInputStream inputStream = new DataInputStream(client.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());

            String request = inputStream.readUTF();
            System.out.println("Client request: " + request);

            String response;
            if (request != null && request.startsWith("REGISTER:")) {
                String[] parts = request.split(":", 3);
                if (parts.length >= 3) {
                    String name = parts[1];
                    String addr = parts[2];
                    addresses.put(name, addr);
                    response = "OK";
                    System.out.println("Registered " + name + " -> " + addr);
                } else {
                    response = "ERROR: invalid REGISTER format";
                    System.out.println("Invalid REGISTER request: " + request);
                }
            } else {
                String requestHost = request;
                String address;
                address = addresses.get(requestHost);
                if (address == null) {
                    System.out.println("Host not found: " + requestHost + "; returning empty string");
                    address = "";
                }
                response = address;
            }

            outputStream.writeUTF(response);

            inputStream.close();
            outputStream.close();
            client.close();
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
