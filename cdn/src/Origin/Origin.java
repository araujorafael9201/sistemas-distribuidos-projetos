package Origin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utils.Crypto;
import Client.Client;

public class Origin {
    public static final String INPUT_ERROR = "Erro na entrada de dados. Tente outra vez!";
    public static final int FILES = 1000;
    public static final int FILE_SIZE = 1024;
    public static final int EDGE_PORT = 8001;

    private static final Pattern FILE = Pattern.compile("arquivo(\\d+)\\.ext");

    static KeyPair keys;

    private int port;

    public Origin(int port) {
        this.port = port;
        run();
    }

    // Devolve N de "arquivo<N>.ext", ou -1 se o nome for inválido ou N estiver fora de 1..1000.
    public static int fileNumber(String file) {
        Matcher m = FILE.matcher(file);
        if (!m.matches()) {
            return -1;
        }
        try {
            int n = Integer.parseInt(m.group(1));
            return n >= 1 && n <= FILES ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void run() {
        try {
            keys = Crypto.newKeyPair();
        } catch (GeneralSecurityException e) {
            System.err.println("Could not generate RSA keys: " + e.getMessage());
            return;
        }

        ServerSocket ss;
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Origin error: " + e.getMessage());
            return;
        }
        System.out.println("Origin listening on TCP port " + port + ", serving arquivo1.ext .. arquivo" + FILES + ".ext");

        Thread server = new Thread(() -> {
            while (true) {
                try {
                    Socket s = ss.accept();
                    new Thread(new OriginImpl(s)).start();
                } catch (IOException e) {
                    System.out.println("Accept error: " + e.getMessage());
                    return;
                }
            }
        });
        server.setDaemon(true);
        server.start();

        menu();
    }

    private void menu() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Commands: invalidate arquivo<N>.ext | sair");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("sair")) {
                break;
            }
            if (!line.startsWith("invalidate ")) {
                System.out.println(INPUT_ERROR);
                continue;
            }
            String file = line.substring("invalidate ".length()).trim();
            if (file.startsWith("/")) {
                file = file.substring(1);
            }
            if (fileNumber(file) < 0) {
                System.out.println(INPUT_ERROR);
                continue;
            }
            invalidate(file);
        }
    }

    // Envia INVALIDATE cifrado para o edge.
    private void invalidate(String file) {
        String msg = "INVALIDATE /" + file;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", EDGE_PORT), 2000);
            socket.setSoTimeout(5000);
            Crypto.Session session = Client.handshake(socket, keys);

            Client.send(socket, Crypto.seal(session, msg));
            System.out.println("Sent to edge " + EDGE_PORT + ": " + msg);

            String resp = Crypto.open(session, Client.receive(socket));
            System.out.println("Edge " + EDGE_PORT + " response (HMAC + RSA signature valid): " + resp);
        } catch (GeneralSecurityException e) {
            System.out.println("Invalid response (HMAC/signature) from edge " + EDGE_PORT + " discarded: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Edge " + EDGE_PORT + " unavailable: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 8000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Origin.Origin [port]");
                return;
            }
        }
        new Origin(port);
    }
}
