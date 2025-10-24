package Calculator;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Calculator {
    int port;
    public Calculator(int port) {
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
                Thread t = new Thread(new CalculatorImpl(s));
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error initializing calculator");
        }
    }

    public static void main(String[] args) {
        new Calculator(8888);
    }
}
