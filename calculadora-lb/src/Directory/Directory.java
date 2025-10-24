package Directory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Directory {
    int port;
    int curRedirectIndex = 0;

    String calculators[] = {
        "127.0.0.1",
        "10.2.3.1",
    };

    public Directory(int port) {
        this.port = port;
        run();
    }

    public void run() {
        try {
            ServerSocket ss = new ServerSocket(port);
            System.out.println("Listening on port " + port);

            while (true) {
                Socket s = ss.accept();
                System.out.println("Connected to client at " + s.getInetAddress().getHostAddress());
                Thread t = new Thread(new DirectoryImpl(s, calculators[curRedirectIndex]));
                t.start();

                curRedirectIndex++;
                curRedirectIndex %= calculators.length;
            }
        } catch (IOException e) {
            System.err.println("Error initializing directory: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Directory(8000);
    }
}

