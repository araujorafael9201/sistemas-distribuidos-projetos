import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int port;

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
                Thread t = new Thread(new ImplServer(socket));
                t.start();
            }
            
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }

    }
 
    public static void main(String[] args) throws Exception {
        Server server = new Server(8888);
    }
}
