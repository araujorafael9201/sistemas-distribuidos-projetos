package Client;
import java.io.IOException;
import java.net.Socket;

public class Client {
    String host;
    int port;
    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        run();
    }

    public void run() {
        try {
            Socket s = new Socket(host, port);
            System.out.println("Succesfully connected to directory on port " + port);

            Thread t = new Thread(new ClientImpl(s));
            t.start();
        } catch (IOException e) {
            System.err.println("Error initializing client: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client("127.0.0.1", 8000);
    }
}


