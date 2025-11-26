import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.io.PrintWriter;
import java.util.ArrayList;

import utils.Logger;
import utils.SensorDTO;

public class Edge {
    private Boolean active = true;
    private Logger logger;
    private final int MAX_CACHE_SIZE = 5;
    private ArrayList<SensorDTO> cache = new ArrayList<>(MAX_CACHE_SIZE); // INSEGURO: ArrayList não trata acesso concorrente

    public Edge(String identifier) {
        logger = new Logger(identifier);
        start();
    }

    public void start() {
        try {
            DatagramSocket udpSocket = new DatagramSocket(8080);
            logger.log("Ouvindo na porta 8080...");

            while (active) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                logger.log("Dados recebidos de dispositivo");
                
                new Thread(() -> processAndForward(received)).start();
            }

            udpSocket.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void processAndForward(String data) {
        SensorDTO sensorData = SensorDTO.fromString(data);
        cache.add(sensorData);

        if (cache.size() > MAX_CACHE_SIZE) {
            logger.log("Tamanho máximo do cache atingido, sincronizando com o servidor");
            flushCache();
        }
    }

    private void flushCache() {
        try {
            Socket socket = new Socket("localhost", 8081);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            for (SensorDTO dto : cache) {
                out.println(dto.toString());
            }
            out.close();
            socket.close();
            cache.clear();
            logger.log("Sincronização com servidor realizada com sucesso, cache limpo");
        } catch (Exception e) {
            logger.log("Erro na sincronização com o servidor, dados atuais em cache serão perdidos. Erro: " + e.getMessage());
            cache.clear();
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Edge("Edge 01");
    }
}
