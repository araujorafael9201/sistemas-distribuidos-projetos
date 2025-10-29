import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Node {
    public int prevPort;
    public int nextPort;
    private int port;
    private String id;
    private boolean running = true;
    private boolean canRequest;

    public Node(int prevPort, int port, int nextPort, String id, boolean canRequest) {
        this.prevPort = prevPort;
        this.nextPort = nextPort;
        this.port = port;
        this.id = id;
        this.canRequest = canRequest;
    }

    public void log(String message) {
        System.out.println("[" + id + "]: " + message);
    }


    public boolean has(String resource) {
        int nodeId = id.charAt(1) - '0';
        
        String resourceNumberStr = resource.substring(7); // Remove "arquivo" prefix
        int resourceNumber;
        
        try {
            resourceNumber = Integer.parseInt(resourceNumberStr);
        } catch (NumberFormatException e) {
            return false;
        }
        
        int rangeStart = (nodeId - 1) * 10 + 11;
        int rangeEnd = nodeId * 10 + 10;
        
        return resourceNumber >= rangeStart && resourceNumber <= rangeEnd;
    }

    public String request(String resource, int ttl) {
        try {
            Socket s = new Socket("127.0.0.1", nextPort);
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            DataInputStream dis = new DataInputStream(s.getInputStream());

            Request req = new Request(resource, ttl);

            dos.writeUTF(req.toString());
            dos.flush();

            String response =  dis.readUTF();
            log("Resposta recebida: " + response);

            s.close();
            return response;
        } catch (IOException e) {
            System.err.println("Erro requisitando recurso: " + e);
        }

        return "";
    }

    public void run() {
        Thread server = new Thread(() -> {
            try {
                ServerSocket ss = new ServerSocket(port);
                log("Escutando na porta " + port);
                while (running) {
                    Socket socket = ss.accept();
                    Thread t = new Thread(new NodeImpl(socket, this));
                    t.start();
                }

                ss.close();
            } catch(IOException e) {
                log("Erro: " + e.getMessage());
            }
        });

        server.setDaemon(true);
        server.start();

        if (canRequest) {
            String input;
            Scanner s = new Scanner(System.in);

            while (true) {
                log("Recurso para requisitar ('sair' para finalizar): ");
                input = s.nextLine();

                if (input.equalsIgnoreCase("sair")) {
                    break;
                }

                log("Requisitando recurso " + input + " para nó na porta " + nextPort);
                request(input, 5);
            }

            s.close();
            running = false;
        }
    }
}
