import java.io.IOException;
import java.net.Socket;

public class Client {
    private String serverAddr;
    private int port;

    public Client(String serverAddr, int port) {
        this.serverAddr = serverAddr;
        this.port = port;

        run();
    }

    public void run() {
        try {
            Socket socket = new Socket(serverAddr, port);
            Thread t = new Thread(new ImplClient(socket));
            t.run();
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client("127.0.0.1", 8888);
    }
}
