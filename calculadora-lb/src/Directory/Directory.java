package Directory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import utils.Crypto;

public class Directory {
    public static final String INPUT_ERROR = "Erro na entrada de dados. Tente outra vez!";

    public static final List<String> SERVICES = Arrays.asList(
        "soma",
        "subtração",
        "multiplicação",
        "divisão"
    );

    static KeyPair keys;
    // nome -> "host:porta", na ordem de registro
    static Map<String, String> servers = new LinkedHashMap<>();
    static int roundRobin = 0;

    private int port;

    public Directory(int port) {
        this.port = port;
        run();
    }

    private void run() {
        try {
            keys = Crypto.newKeyPair();
        } catch (GeneralSecurityException e) {
            System.err.println("Could not generate RSA keys: " + e.getMessage());
            return;
        }

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Listening on TCP port " + port);
            while (true) {
                Socket s = ss.accept();
                new Thread(new DirectoryImpl(s)).start();
            }
        } catch (IOException e) {
            System.err.println("Directory error: " + e.getMessage());
        }
    }

    // Registra (ou substitui) um servidor de calculadora.
    static synchronized void register(String name, String address) {
        servers.put(name, address);
        System.out.println("Registered " + name + " -> " + address + " (" + servers.size() + " servers)");
    }

    // Devolve os servidores rotacionados pelo contador global de round robin.
    static synchronized List<String> lookup() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> e : servers.entrySet()) {
            list.add(e.getKey() + ":" + e.getValue());
        }
        if (list.isEmpty()) {
            return list;
        }
        int shift = roundRobin % list.size();
        roundRobin++;
        List<String> rotated = new ArrayList<>(list.subList(shift, list.size()));
        rotated.addAll(list.subList(0, shift));
        System.out.println("Round robin #" + roundRobin + ": " + rotated);
        return rotated;
    }

    public static void main(String[] args) {
        int port = 8000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -cp bin Directory [port]");
                return;
            }
        }
        new Directory(port);
    }
}
