package Edge;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

import utils.Crypto;
import Client.Client;
import Origin.Origin;

public class EdgeImpl implements Runnable {
    private Socket socket;
    private String from;
    private String originAddr;
    private int originPort;

    public EdgeImpl(Socket socket, String originAddr, int originPort) {
        this.socket = socket;
        this.from = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.originAddr = originAddr;
        this.originPort = originPort;
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
                // Fecha a conexão sem responder e sem mexer no cache.
                Edge.log("ALERT: integrity failure (" + e.getMessage() + ") from " + from
                        + ", closing connection (cache untouched)");
                return;
            }
            // Clientes mandam a região na segunda linha: "GET /arquivo\nRegion: SUL"
            String[] lines = request.split("\n", 2);
            request = lines[0];
            if (lines.length > 1 && lines[1].startsWith("Region: ")) {
                from = "client in " + lines[1].substring("Region: ".length()) + " (" + from + ")";
            }
            Edge.log("Received from " + from + " (HMAC + RSA signature valid): " + request);

            String response = handle(request);
            if (request.startsWith("GET /")) {
                // Identifica qual edge respondeu, logo abaixo da linha de status
                int nl = response.indexOf('\n');
                response = nl < 0 ? response + "\nEdge: " + Edge.label
                        : response.substring(0, nl) + "\nEdge: " + Edge.label + response.substring(nl);
            }
            Client.send(socket, Crypto.seal(session, response));
            Edge.log("Sent to " + from + ": " + response.split("\n\n")[0].replace("\n", " | "));
        } catch (GeneralSecurityException e) {
            Edge.log("ALERT: handshake failed for " + from + ": " + e.getMessage());
        } catch (IOException e) {
            Edge.log("Connection error with " + from + ": " + e.getMessage());
        }
    }

    // Envia a chave pública, recebe as chaves AES + HMAC cifradas com RSA e confirma com READY.
    private Crypto.Session handshake() throws IOException, GeneralSecurityException {
        Edge.log("Handshake started by " + from + ", sending public key");
        Client.send(socket, Edge.keys.getPublic().getEncoded());
        Crypto.Session session = Crypto.unwrapKeys(Edge.keys.getPrivate(), Client.receive(socket));
        Edge.log("Session keys (AES + HMAC) and client RSA public key received from " + from);
        Client.send(socket, Crypto.seal(session, "READY"));
        return session;
    }

    private String handle(String request) {
        if (request.startsWith("INVALIDATE /")) {
            String file = request.substring("INVALIDATE /".length());
            if (Edge.cache.remove(file) != null) {
                Edge.log("INVALIDATE: " + file + " removed from cache");
            } else {
                Edge.log("INVALIDATE: " + file + " was not in cache");
            }
            return "OK";
        }

        if (!request.startsWith("GET /")) {
            Edge.log("Unknown request: " + request);
            return "400 Bad Request\n\n" + Origin.INPUT_ERROR;
        }

        // Valida antes de consultar o cache ou a Origem
        String file = request.substring("GET /".length());
        if (Origin.fileNumber(file) < 0) {
            Edge.log("Invalid file name: " + file + " (origin not contacted)");
            return "400 Bad Request\n\n" + Origin.INPUT_ERROR;
        }

        if (Edge.cached(file)) {
            Edge.log("Cache HIT: " + file);
            return "200 OK\nCache: HIT\nLength: " + Origin.FILE_SIZE;
        }

        Edge.log("Cache MISS: " + file + ", fetching from origin");
        String resp;
        try {
            resp = Edge.fetch(originAddr, originPort, file);
        } catch (GeneralSecurityException e) {
            Edge.log("Invalid response (HMAC/signature) from origin discarded: " + e.getMessage());
            return "502 Bad Gateway\n\nInvalid response from origin";
        } catch (IOException e) {
            Edge.log("Origin unavailable: " + e.getMessage());
            return "503 Service Unavailable\n\nOrigin unavailable";
        }
        if (!resp.startsWith("200 OK")) {
            return resp;
        }

        Edge.cache.put(file, System.currentTimeMillis());
        Edge.log(file + " saved in cache");
        return "200 OK\nCache: MISS\nLength: " + Origin.FILE_SIZE;
    }
}
