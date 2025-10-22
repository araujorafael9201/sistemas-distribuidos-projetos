import java.io.IOException;
import java.net.Socket;

public class Registrator {
    private String serverAddr;
    private int port;

    public Registrator(String serverAddr, int port) {
        this.serverAddr = serverAddr;
        this.port = port;

        run();
    }

    public void run() {
        try {
            Socket socket = new Socket(serverAddr, port);
            Thread t = new Thread(new ImplRegistrator(socket));
            t.start();
        } catch (IOException e) {
            System.out.println("Registrator error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
       new Registrator("127.0.0.1", 8888);
    }
}
