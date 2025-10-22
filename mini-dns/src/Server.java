import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {
    private int port;
    // ConcurrentHashMap é seguro para acesso concorrente
    // Aqui é usado HashMap + bloco synchronized na thread
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

    public Server(int port) {
        this.port = port;
        run();
    }

    private void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            System.out.println("Listening on port 8888");
            int i = 0;
            while (true) {
                i += 1;
                if (i == 10) {
                    break;
                }
                Socket socket = serverSocket.accept();
                Thread t = new Thread(new ImplServer(socket, addresses));
                t.start();
            }
            
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }

    }
 
    public static void main(String[] args) throws Exception {
        new Server(8888);
    }
}
