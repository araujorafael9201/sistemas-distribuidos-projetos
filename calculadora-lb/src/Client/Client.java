package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Scanner;

import utils.Crypto;

public class Client {
    private static final int MAX_MESSAGE = 64 * 1024;

    private String directoryAddr;
    private int directoryPort;

    public Client(String directoryAddr, int directoryPort) {
        this.directoryAddr = directoryAddr;
        this.directoryPort = directoryPort;
        run();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Commands: <soma|subtração|multiplicação|divisão> <a> <b> | sair");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("sair")) {
                break;
            }
            Thread t = new Thread(new ClientImpl(directoryAddr, directoryPort, line));
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println("Client interrupted");
                break;
            }
        }
    }

    // Handshake RSA: recebe a chave pública, envia as chaves AES + HMAC cifradas e espera o READY.
    public static Crypto.Session handshake(Socket socket) throws IOException, GeneralSecurityException {
        byte[] publicKey = receive(socket);

        Crypto.Session session = Crypto.newSession();
        send(socket, Crypto.wrapKeys(publicKey, session));

        String ready = Crypto.open(session, receive(socket));
        if (!ready.equals("READY")) {
            throw new GeneralSecurityException("unexpected handshake reply: " + ready);
        }
        System.out.println("Secure channel established with " + socket.getInetAddress().getHostAddress()
                + ":" + socket.getPort() + " (HMAC valid)");
        return session;
    }

    public static void send(Socket socket, byte[] payload) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    public static byte[] receive(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        int length = in.readInt();
        if (length <= 0 || length > MAX_MESSAGE) {
            throw new IOException("unexpected message from peer");
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = 8000;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Client [host] [port]");
                return;
            }
        }
        new Client(host, port);
    }
}
