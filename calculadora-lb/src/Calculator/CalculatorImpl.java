package Calculator;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

import utils.Crypto;
import Client.Client;

public class CalculatorImpl implements Runnable {
    private Socket socket;
    private String from;

    public CalculatorImpl(Socket socket) {
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
                System.out.println("[" + Calculator.name + "] Integrity failure (HMAC) from " + from + ": message discarded");
                return;
            }
            System.out.println("[" + Calculator.name + "] Received from " + from + " (HMAC valid): " + request);

            // O cálculo em si não importa aqui: o servidor só confirma quem atendeu a chamada.
            String response = "OK:" + Calculator.name + " handled \"" + request + "\"";
            if (Calculator.tamper) {
                Client.send(socket, Crypto.corrupt(Crypto.seal(session, response)));
                System.out.println("[" + Calculator.name + "] Sent tampered reply to " + from + ": " + response);
            } else {
                Client.send(socket, Crypto.seal(session, response));
                System.out.println("[" + Calculator.name + "] Sent to " + from + ": " + response);
            }
        } catch (GeneralSecurityException e) {
            System.out.println("[" + Calculator.name + "] Handshake failed for " + from + ": " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[" + Calculator.name + "] Connection error with " + from + ": " + e.getMessage());
        }
    }

    // Envia a chave pública, recebe as chaves AES + HMAC cifradas com RSA e confirma com READY.
    private Crypto.Session handshake() throws IOException, GeneralSecurityException {
        System.out.println("[" + Calculator.name + "] Handshake started by " + from + ", sending public key");
        Client.send(socket, Calculator.keys.getPublic().getEncoded());
        Crypto.Session session = Crypto.unwrapKeys(Calculator.keys.getPrivate(), Client.receive(socket));
        System.out.println("[" + Calculator.name + "] Session keys (AES + HMAC) received from " + from);
        Client.send(socket, Crypto.seal(session, "READY"));
        return session;
    }
}
