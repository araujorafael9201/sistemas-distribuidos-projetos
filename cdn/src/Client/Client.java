package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Scanner;

import utils.Crypto;
import Router.Router;

public class Client {
    private static final int MAX_MESSAGE = 64 * 1024;

    static KeyPair keys;

    private SocketAddress router;
    private Scanner scanner;

    public Client(String routerAddr, int routerPort, Scanner scanner) {
        this.router = new InetSocketAddress(routerAddr, routerPort);
        this.scanner = scanner;
        run();
    }

    private void run() {
        try {
            keys = Crypto.newKeyPair();
        } catch (GeneralSecurityException e) {
            System.err.println("Could not generate RSA keys: " + e.getMessage());
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);
            Crypto.Session session = handshake(socket, router);
            menu(socket, session);
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private void menu(DatagramSocket socket, Crypto.Session session) {
        System.out.println("Commands: arquivo<N>.ext | corrupt arquivo<N>.ext | sair");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("sair")) {
                break;
            }
            boolean corrupt = line.startsWith("corrupt ");
            String file = corrupt ? line.substring("corrupt ".length()).trim() : line;

            Thread t = new Thread(new ClientImpl(socket, router, session, file, corrupt));
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println("Client interrupted");
                break;
            }
        }
    }

    // Handshake RSA com o Router por UDP: HELLO, recebe a chave pública, envia as chaves
    // AES + HMAC cifradas junto com a própria chave pública e espera o READY.
    public static Crypto.Session handshake(DatagramSocket socket, SocketAddress router)
            throws IOException, GeneralSecurityException {
        send(socket, router, Router.HELLO, new byte[0]);
        byte[] publicKey = receive(socket, Router.KEY);

        Crypto.Session session = Crypto.newSession(keys);
        send(socket, router, Router.SESSION, Crypto.wrapKeys(publicKey, keys, session));

        String ready = Crypto.open(session, receive(socket, Router.DATA));
        if (!ready.equals("READY")) {
            throw new GeneralSecurityException("unexpected handshake reply: " + ready);
        }
        System.out.println("Secure channel established with router " + router + " (HMAC + RSA signature valid)");
        return session;
    }

    // Handshake RSA por TCP: recebe a chave pública, envia as chaves AES + HMAC cifradas junto
    // com a própria chave pública (para as assinaturas) e espera o READY.
    public static Crypto.Session handshake(Socket socket, KeyPair mine) throws IOException, GeneralSecurityException {
        byte[] publicKey = receive(socket);

        Crypto.Session session = Crypto.newSession(mine);
        send(socket, Crypto.wrapKeys(publicKey, mine, session));

        String ready = Crypto.open(session, receive(socket));
        if (!ready.equals("READY")) {
            throw new GeneralSecurityException("unexpected handshake reply: " + ready);
        }
        System.out.println("Secure channel established with " + socket.getInetAddress().getHostAddress()
                + ":" + socket.getPort() + " (HMAC + RSA signature valid)");
        return session;
    }

    public static void send(DatagramSocket socket, SocketAddress to, byte tag, byte[] payload) throws IOException {
        byte[] data = Crypto.concat(new byte[]{tag}, payload);
        socket.send(new DatagramPacket(data, data.length, to));
    }

    public static byte[] receive(DatagramSocket socket, byte tag) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
        socket.receive(packet);
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
        if (data.length == 0 || data[0] != tag) {
            throw new IOException("unexpected packet from router");
        }
        return Arrays.copyOfRange(data, 1, data.length);
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
        int port = 8081;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Client.Client [host] [port]");
                return;
            }
        }

        new Client(host, port, new Scanner(System.in));
    }
}
