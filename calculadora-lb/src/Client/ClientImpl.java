package Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

import utils.Crypto;

public class ClientImpl implements Runnable {
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int READ_TIMEOUT = 5000;

    private String directoryAddr;
    private int directoryPort;
    private String line;

    public ClientImpl(String directoryAddr, int directoryPort, String line) {
        this.directoryAddr = directoryAddr;
        this.directoryPort = directoryPort;
        this.line = line;
    }

    @Override
    public void run() {
        String service = line.split("\\s+")[0];

        // 1. Descobre os servidores no diretório
        String lookup;
        try {
            lookup = call(directoryAddr, directoryPort, "LOOKUP:" + service);
        } catch (GeneralSecurityException e) {
            System.out.println("Invalid message (HMAC/signature) from directory discarded: " + e.getMessage());
            return;
        } catch (IOException e) {
            System.out.println("Directory unavailable: " + e.getMessage());
            return;
        }
        if (lookup.startsWith("ERR:")) {
            System.out.println(lookup.substring("ERR:".length()));
            return;
        }
        String[] servers = lookup.substring("OK:".length()).split(",");

        // 2. Tenta os servidores na ordem recebida (nome:host:porta), pulando os inacessíveis
        for (String server : servers) {
            String[] parts = server.split(":");
            String name = parts[0];
            String host = parts[1];
            int port = Integer.parseInt(parts[2]);

            System.out.println("Trying " + name + " at " + host + ":" + port);
            try {
                String response = call(host, port, line);
                System.out.println("Reply from " + name + ": " + response.substring("OK:".length()));
                return;
            } catch (GeneralSecurityException e) {
                System.out.println("Invalid message (HMAC/signature) from " + name + " discarded, call aborted: " + e.getMessage());
                return;
            } catch (IOException e) {
                System.out.println(name + " unavailable (" + e.getMessage() + "), trying next server");
            }
        }
        System.out.println("No calculator server available");
    }

    // Abre uma conexão nova, faz o handshake, envia uma mensagem e devolve a resposta.
    private String call(String host, int port, String message) throws IOException, GeneralSecurityException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            Crypto.Session session = Client.handshake(socket, Client.keys);

            Client.send(socket, Crypto.seal(session, message));
            System.out.println("Sent: " + message);

            String response = Crypto.open(session, Client.receive(socket));
            System.out.println("Received (HMAC + RSA signature valid): " + response);
            return response;
        }
    }
}
