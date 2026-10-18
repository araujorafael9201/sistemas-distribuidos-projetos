package Directory;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.List;

import utils.Crypto;
import Client.Client;

public class DirectoryImpl implements Runnable {
    private Socket socket;
    private String from;

    public DirectoryImpl(Socket socket) {
        this.socket = socket;
        this.from = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        try (Socket s = socket) {
            Crypto.Session session = handshake();

            String request;
            try {
                request = Crypto.open(session, Client.receive(socket));
            } catch (GeneralSecurityException e) {
                System.out.println("Integrity failure (HMAC) from " + from + ": message discarded");
                return;
            }
            System.out.println("Received from " + from + " (HMAC valid): " + request);

            String response = handle(request);
            Client.send(socket, Crypto.seal(session, response));
            System.out.println("Sent to " + from + ": " + response);
        } catch (GeneralSecurityException e) {
            System.out.println("Handshake failed for " + from + ": " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Connection error with " + from + ": " + e.getMessage());
        }
    }

    // Envia a chave pública, recebe as chaves AES + HMAC cifradas com RSA e confirma com READY.
    private Crypto.Session handshake() throws IOException, GeneralSecurityException {
        System.out.println("Handshake started by " + from + ", sending public key");
        Client.send(socket, Directory.keys.getPublic().getEncoded());
        Crypto.Session session = Crypto.unwrapKeys(Directory.keys.getPrivate(), Client.receive(socket));
        System.out.println("Session keys (AES + HMAC) received from " + from);
        Client.send(socket, Crypto.seal(session, "READY"));
        return session;
    }

    private String handle(String request) {
        if (request.startsWith("REGISTER:")) {
            String[] parts = request.split(":", 3);
            if (parts.length != 3 || parts[1].isEmpty()) {
                System.out.println("Invalid REGISTER: " + request);
                return "ERR:" + Directory.INPUT_ERROR;
            }
            try {
                int port = Integer.parseInt(parts[2]);
                Directory.register(parts[1], socket.getInetAddress().getHostAddress() + ":" + port);
                return "OK";
            } catch (NumberFormatException e) {
                System.out.println("Invalid REGISTER: " + request);
                return "ERR:" + Directory.INPUT_ERROR;
            }
        }

        if (request.startsWith("LOOKUP:")) {
            String service = request.substring("LOOKUP:".length());
            if (!Directory.SERVICES.contains(service)) {
                System.out.println("Lookup failed: " + service);
                return "ERR:" + Directory.INPUT_ERROR;
            }
            List<String> servers = Directory.lookup();
            if (servers.isEmpty()) {
                return "ERR:no servers available";
            }
            return "OK:" + String.join(",", servers);
        }

        System.out.println("Unknown request: " + request);
        return "ERR:" + Directory.INPUT_ERROR;
    }
}
