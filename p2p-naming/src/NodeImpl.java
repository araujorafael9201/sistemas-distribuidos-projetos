import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NodeImpl implements Runnable {
    private Socket client;
    private Node parent;

    public NodeImpl(Socket client, Node parent) {
        this.client = client;
        this.parent = parent;
    }

    @Override
    public void run() {
        parent.log("Conectado ao cliente: " + client.getInetAddress().getHostName());

        try {
            DataInputStream dis = new DataInputStream(client.getInputStream());
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());

            String requestString = dis.readUTF();

            String response = "response";
            Request req = Request.fromString(requestString);

            if (req.getResource().startsWith("arquivo")) {
                if (req.getTtl() <= 0) {
                    parent.log("TTL Excedido, passando resposta para o nó anterior na porta " + parent.prevPort);
                    dos.writeUTF("Recurso não encontrado (TTL Excedido)");
                    return;
                }

                if (!parent.has(req.getResource())) {
                    parent.log("Recurso não encontrado, repassando requisição para o próximo nó na porta " + parent.nextPort);
                    response = parent.request(req.getResource(), req.getTtl() - 1);
                } else {
                    parent.log("Recurso encontrado, passando resposta para o nó anterior na porta " + parent.prevPort);
                    response = "Recurso encontrado no nó da porta " + parent.port;
                }
            } else {
                parent.log("Erro na entrada de dados. Tente outra vez!");
                response = "Erro na entrada de dados. Tente outra vez";
            }

            dos.writeUTF(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
