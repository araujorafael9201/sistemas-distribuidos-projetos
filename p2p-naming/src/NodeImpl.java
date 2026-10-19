import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

// Atende uma conexão recebida: uma busca vinda do nó anterior ou a resposta do dono de um arquivo.
public class NodeImpl implements Runnable {
    private Socket socket;

    public NodeImpl(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String message;
        try (Socket s = socket) {
            Crypto.Session session = handshake();

            try {
                message = Crypto.open(session, Node.receive(socket));
            } catch (GeneralSecurityException e) {
                Node.log("Integrity failure (" + e.getMessage() + "): message discarded, not forwarding");
                return;
            }
            Node.log("Received (HMAC + RSA signature valid): " + message);
            Node.send(socket, Crypto.seal(session, "ACK"));
        } catch (GeneralSecurityException e) {
            Node.log("Handshake failed: " + e.getMessage());
            return;
        } catch (IOException e) {
            Node.log("Connection error: " + e.getMessage());
            return;
        }

        // Resposta do dono do arquivo para este nó (a origem da busca)
        if (message.startsWith("FOUND:")) {
            String[] parts = message.split(":");
            Node.log("Result: " + parts[1] + " is at " + parts[2]);
            return;
        }
        if (message.startsWith("ERR:")) {
            Node.log("Search failed: " + message.substring("ERR:".length()));
            return;
        }

        Request req;
        try {
            req = Request.fromString(message);
        } catch (RuntimeException e) {
            Node.log("Invalid request: " + message);
            return;
        }

        String file = req.getResource();
        int origin = req.getOrigin();
        if (Node.has(file)) {
            Node.log(file + " found here, answering P" + origin + " directly");
            Node.deliver(origin, "FOUND:" + file + ":P" + Node.id, false);
        } else if (req.getTtl() <= 0) {
            Node.log("TTL expired for " + file);
            Node.deliver(origin, "ERR:" + file + " not found", false);
        } else {
            Node.log(file + " not here, re-encrypting and forwarding to P" + Node.next());
            if (!Node.deliver(Node.next(), new Request(file, origin, req.getTtl() - 1).toString(), false)) {
                Node.deliver(origin, "ERR:search stopped at P" + Node.id, false);
            }
        }
    }

    // Envia a chave pública, recebe as chaves AES + HMAC cifradas com RSA e confirma com READY.
    private Crypto.Session handshake() throws IOException, GeneralSecurityException {
        Node.log("Handshake started by incoming connection, sending public key");
        Node.send(socket, Node.keys.getPublic().getEncoded());
        Crypto.Session session = Crypto.unwrapKeys(Node.keys.getPrivate(), Node.receive(socket));
        Node.log("Session keys (AES + HMAC) and peer RSA public key received");
        Node.send(socket, Crypto.seal(session, "READY"));
        return session;
    }
}
