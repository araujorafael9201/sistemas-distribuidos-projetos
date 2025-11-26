package Edge;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import utils.Logger;
import utils.SensorDTO;

public class Edge {
    private Boolean active = true;
    private Logger logger;
    private final int MAX_CACHE_SIZE = 5;
    private EdgeCache cache = new EdgeCache(MAX_CACHE_SIZE);

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
    }

    public static void main(String[] args) {
        new Edge("Edge 01");
    }
}
