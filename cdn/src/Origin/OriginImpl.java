package Origin;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

import utils.Crypto;
import Client.Client;

public class OriginImpl implements Runnable {
    private Socket socket;
    private String from;

    public OriginImpl(Socket socket) {
        this.socket = socket;
        this.from = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        try (Socket s = socket) {
            socket.setSoTimeout(10000);
            Crypto.Session session = handshake();

            String request;
            try {
                request = Crypto.open(session, Client.receive(socket));
            } catch (GeneralSecurityException e) {
                System.out.println("Integrity failure (" + e.getMessage() + ") from " + from + ": message discarded");
                return;
            }
            System.out.println("Received from " + from + " (HMAC + RSA signature valid): " + request);

            String response = handle(request);
            Client.send(socket, Crypto.seal(session, response));
            System.out.println("Sent to " + from + ": " + response.replace("\n", " | "));
        } catch (GeneralSecurityException e) {
            System.out.println("Handshake failed for " + from + ": " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Connection error with " + from + ": " + e.getMessage());
        }
    }

    // Envia a chave pública, recebe as chaves AES + HMAC cifradas com RSA e confirma com READY.
    private Crypto.Session handshake() throws IOException, GeneralSecurityException {
        System.out.println("Handshake started by " + from + ", sending public key");
        Client.send(socket, Origin.keys.getPublic().getEncoded());
        Crypto.Session session = Crypto.unwrapKeys(Origin.keys.getPrivate(), Client.receive(socket));
        System.out.println("Session keys (AES + HMAC) and client RSA public key received from " + from);
        Client.send(socket, Crypto.seal(session, "READY"));
        return session;
    }

    private String handle(String request) {
        if (!request.startsWith("GET /")) {
            System.out.println("Unknown request: " + request);
            return "400 Bad Request\n\n" + Origin.INPUT_ERROR;
        }
        String file = request.substring("GET /".length());
        if (Origin.fileNumber(file) < 0) {
            System.out.println("File not found: " + file);
            return "404 Not Found\n\n" + Origin.INPUT_ERROR;
        }
        System.out.println("Found " + file);
        return "200 OK\nLength: " + Origin.FILE_SIZE;
    }
}
