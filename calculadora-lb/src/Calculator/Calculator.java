package Calculator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Scanner;

import utils.Crypto;
import Client.Client;
import Directory.Directory;

public class Calculator {
    static String name;
    static boolean tamper;
    static KeyPair keys;

    private int port;
    private String directoryAddr;
    private int directoryPort;

    public Calculator(int port, String directoryAddr, int directoryPort) {
        this.port = port;
        this.directoryAddr = directoryAddr;
        this.directoryPort = directoryPort;
        run();
    }

    private void run() {
        try {
            keys = Crypto.newKeyPair();
        } catch (GeneralSecurityException e) {
            System.err.println("Could not generate RSA keys: " + e.getMessage());
            return;
        }

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println(name + " listening on TCP port " + port + (tamper ? " (TAMPER: every reply is corrupted)" : ""));
            register();
            while (true) {
                Socket s = ss.accept();
                new Thread(new CalculatorImpl(s)).start();
            }
        } catch (IOException e) {
            System.err.println("Calculator error: " + e.getMessage());
        }
    }

    // Registra o servidor no diretório por um canal cifrado.
    private void register() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(directoryAddr, directoryPort), 3000);
            Crypto.Session session = Client.handshake(socket);

            String msg = "REGISTER:" + name + ":" + port;
            Client.send(socket, Crypto.seal(session, msg));
            System.out.println("Sent: " + msg);

            String resp = Crypto.open(session, Client.receive(socket));
            System.out.println("Directory response (HMAC valid): " + resp);
        } catch (GeneralSecurityException e) {
            System.out.println("Invalid response (HMAC) discarded: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not register with directory: " + e.getMessage());
        }
    }

    // Lê uma linha do terminal; devolve null se a entrada acabou.
    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        name = ask(scanner, "Server name (e.g. S1): ");
        while (name != null && name.isEmpty()) {
            System.out.println(Directory.INPUT_ERROR);
            name = ask(scanner, "Server name (e.g. S1): ");
        }

        Integer port = null;
        while (port == null && name != null) {
            String line = ask(scanner, "Port (e.g. 9001): ");
            if (line == null) {
                break;
            }
            try {
                port = Integer.parseInt(line);
                if (port < 1 || port > 65535) {
                    port = null;
                    System.out.println(Directory.INPUT_ERROR);
                }
            } catch (NumberFormatException e) {
                System.out.println(Directory.INPUT_ERROR);
            }
        }

        String answer = port == null ? null : ask(scanner, "Corrupt every reply (tamper)? [s/N]: ");
        if (answer == null) {
            System.out.println("No input, exiting");
            return;
        }
        tamper = answer.equalsIgnoreCase("s");

        new Calculator(port, "127.0.0.1", 8000);
    }
}
