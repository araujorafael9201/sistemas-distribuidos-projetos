import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node {
    public static final String INPUT_ERROR = "Erro na entrada de dados. Tente outra vez!";
    public static final int NODES = 6;
    public static final int BASE_PORT = 8881;

    private static final int MAX_MESSAGE = 64 * 1024;
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int READ_TIMEOUT = 10000;
    private static final Pattern FILE = Pattern.compile("arquivo(\\d+)\\.ext");

    static int id;
    static KeyPair keys;

    private Scanner scanner;

    public Node(int id, Scanner scanner) {
        Node.id = id;
        this.scanner = scanner;
        run();
    }

    static int next() {
        return (id + 1) % NODES;
    }

    static int prev() {
        return (id + NODES - 1) % NODES;
    }

    static void log(String message) {
        System.out.println("[P" + id + "] " + message);
    }

    // Devolve N de "arquivo<N>.ext", ou -1 se o nome for inválido ou N estiver fora de 1..60.
    static int fileNumber(String file) {
        Matcher m = FILE.matcher(file);
        if (!m.matches()) {
            return -1;
        }
        try {
            int n = Integer.parseInt(m.group(1));
            return n >= 1 && n <= NODES * 10 ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // O nó id guarda os arquivos id*10+1 .. id*10+10.
    static boolean has(String file) {
        int n = fileNumber(file);
        return n >= id * 10 + 1 && n <= id * 10 + 10;
    }

    private void run() {
        try {
            keys = Crypto.newKeyPair();
        } catch (GeneralSecurityException e) {
            System.err.println("Could not generate RSA keys: " + e.getMessage());
            return;
        }

        ServerSocket ss;
        try {
            ss = new ServerSocket(BASE_PORT + id);
        } catch (IOException e) {
            System.err.println("Node error: " + e.getMessage());
            return;
        }
        log("Listening on TCP port " + (BASE_PORT + id) + ", next node is P" + next() + " (port " + (BASE_PORT + next()) + ")");

        Thread server = new Thread(() -> {
            while (true) {
                try {
                    Socket s = ss.accept();
                    new Thread(new NodeImpl(s)).start();
                } catch (IOException e) {
                    log("Accept error: " + e.getMessage());
                    return;
                }
            }
        });
        server.setDaemon(true);
        server.start();

        menu();
    }

    private void menu() {
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

            // Valida antes de qualquer criptografia ou conexão
            if (fileNumber(file) < 0) {
                System.out.println(INPUT_ERROR);
                continue;
            }
            if (has(file)) {
                log(file + " found locally");
                continue;
            }

            // A resposta não volta por aqui: o dono do arquivo responde direto para este nó.
            if (deliver(next(), new Request(file, id, NODES - 1).toString(), corrupt)) {
                log("Search for " + file + " sent, waiting for the owner's answer");
            } else {
                log("Search for " + file + " failed");
            }
        }
    }

    // Abre uma conexão nova com outro nó, faz o handshake, envia a mensagem e espera o ACK.
    static boolean deliver(int peer, String text, boolean corrupt) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", BASE_PORT + peer), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            Crypto.Session session = handshake(socket, peer);

            byte[] message = Crypto.seal(session, text);
            if (corrupt) {
                send(socket, Crypto.corrupt(message));
                log("Sent tampered packet to P" + peer + ": " + text + " (P" + peer + " should discard it)");
            } else {
                send(socket, message);
                log("Sent to P" + peer + ": " + text);
            }

            String ack = Crypto.open(session, receive(socket));
            return ack.equals("ACK");
        } catch (EOFException e) {
            log("No reply from P" + peer + " (connection closed)");
        } catch (GeneralSecurityException e) {
            log("Invalid message (HMAC/signature) from P" + peer + " discarded: " + e.getMessage());
        } catch (IOException e) {
            log("P" + peer + " unavailable: " + e.getMessage());
        }
        return false;
    }

    // Handshake RSA: recebe a chave pública, envia as chaves AES + HMAC cifradas junto com a
    // própria chave pública (para as assinaturas) e espera o READY.
    static Crypto.Session handshake(Socket socket, int peer) throws IOException, GeneralSecurityException {
        byte[] publicKey = receive(socket);

        Crypto.Session session = Crypto.newSession(keys);
        send(socket, Crypto.wrapKeys(publicKey, keys, session));

        String ready = Crypto.open(session, receive(socket));
        if (!ready.equals("READY")) {
            throw new GeneralSecurityException("unexpected handshake reply: " + ready);
        }
        log("Secure channel established with P" + peer + " (HMAC + RSA signature valid)");
        return session;
    }

    // TCP: cada mensagem vai como writeInt(tamanho) + bytes.
    static void send(Socket socket, byte[] payload) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    static byte[] receive(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        int length = in.readInt();
        if (length <= 0 || length > MAX_MESSAGE) {
            throw new IOException("unexpected message from peer");
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }
}
