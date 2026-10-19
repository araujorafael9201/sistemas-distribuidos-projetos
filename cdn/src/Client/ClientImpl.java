package Client;

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;

import utils.Crypto;
import Router.Router;

public class ClientImpl implements Runnable {
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int READ_TIMEOUT = 10000;

    private DatagramSocket socket;
    private SocketAddress router;
    private Crypto.Session session;
    private String location;
    private String file;
    private boolean corrupt;

    public ClientImpl(DatagramSocket socket, SocketAddress router, Crypto.Session session,
            String location, String file, boolean corrupt) {
        this.socket = socket;
        this.router = router;
        this.session = session;
        this.location = location;
        this.file = file;
        this.corrupt = corrupt;
    }

    @Override
    public void run() {
        // 1. Pergunta ao Router (UDP cifrado) qual edge atende esta localização
        String where;
        try {
            String msg = "WHERE:" + location + ":" + file;
            Client.send(socket, router, Router.DATA, Crypto.seal(session, msg));
            System.out.println("Sent to router: " + msg);
            where = Crypto.open(session, Client.receive(socket, Router.DATA));
            System.out.println("Router response (HMAC + RSA signature valid): " + where);
        } catch (GeneralSecurityException e) {
            System.out.println("Invalid message (HMAC/signature) from router discarded: " + e.getMessage());
            return;
        } catch (IOException e) {
            System.out.println("Router unavailable: " + e.getMessage());
            return;
        }
        if (where.startsWith("ERR:")) {
            System.out.println(where.substring("ERR:".length()));
            return;
        }
        String[] parts = where.split(":");
        int port;
        try {
            if (parts.length != 3 || !parts[0].equals("OK")) {
                throw new NumberFormatException();
            }
            port = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            System.out.println("Unexpected router response: " + where);
            return;
        }
        String host = parts[1];
        String edgeLabel = Router.edgeLabel(port);
        System.out.println("Client in " + location + " -> edge " + edgeLabel + " at " + host + ":" + port);

        // 2. Pede o arquivo ao edge (TCP cifrado)
        try (Socket edge = new Socket()) {
            edge.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
            edge.setSoTimeout(READ_TIMEOUT);
            Crypto.Session edgeSession = Client.handshake(edge, Client.keys);

            String request = "GET /" + file + "\nRegion: " + location;
            long start = System.currentTimeMillis();
            if (corrupt) {
                Client.send(edge, Crypto.corrupt(Crypto.seal(edgeSession, request)));
                System.out.println("Sent tampered packet to edge " + edgeLabel + ": GET /" + file + " (the edge should close the connection)");
            } else {
                Client.send(edge, Crypto.seal(edgeSession, request));
                System.out.println("Sent to edge " + edgeLabel + ": GET /" + file + " (Region: " + location + ")");
            }

            String response = Crypto.open(edgeSession, Client.receive(edge));
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Received (HMAC + RSA signature valid) in " + elapsed + " ms:");
            print(response);
        } catch (EOFException e) {
            System.out.println("Edge " + edgeLabel + " closed the connection without replying");
        } catch (GeneralSecurityException e) {
            System.out.println("Invalid message (HMAC/signature) from edge " + edgeLabel + " discarded: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Edge " + edgeLabel + " unavailable: " + e.getMessage());
        }
    }

    // Mostra o status e os cabeçalhos; nas respostas de erro, o texto vem depois da linha em branco.
    private void print(String response) {
        int split = response.indexOf("\n\n");
        String head = split < 0 ? response : response.substring(0, split);
        for (String line : head.split("\n")) {
            System.out.println("  " + line);
        }
        if (split >= 0) {
            System.out.println(response.substring(split + 2));
        }
    }
}
