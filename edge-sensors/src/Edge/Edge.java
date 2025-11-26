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
        verifyData(sensorData);
        cache.add(sensorData);
    }

    private void verifyData(SensorDTO data) {
        if (data.getTemperatura() > 30) logger.log("ALERTA: Temperatura alta: " + String.format("%.2f", data.getTemperatura()));
        if (data.getCo2() > 900) logger.log("ALERTA: Níveis de CO2 altos: " + String.format("%.2f", data.getCo2()));
        if (data.getCo() > 9) logger.log("ALERTA: Níveis de CO altos: " + String.format("%.2f", data.getCo()));
        if (data.getNo2() > 0.09) logger.log("ALERTA: Níveis de NO2 altos: " + String.format("%.2f", data.getNo2()));
        if (data.getSo2() > 0.04) logger.log("ALERTA: Níveis de SO2 altos: " + String.format("%.2f", data.getSo2()));
        if (data.getPm5() > 90) logger.log("ALERTA: PM2.5 alto: " + String.format("%.2f", data.getPm5()));
        if (data.getPm10() > 140) logger.log("ALERTA: PM10 alto: " + String.format("%.2f", data.getPm10()));
        if (data.getUmidade() < 25) logger.log("ALERTA: Umidade baixa: " + String.format("%.2f", data.getUmidade()));
        if (data.getRuido() > 85) logger.log("ALERTA: Ruído alto: " + String.format("%.2f", data.getRuido()));
        if (data.getRadiacaoUV() > 10) logger.log("ALERTA: Radiação UV alta: " + String.format("%.2f", data.getRadiacaoUV()));
    }

    public static void main(String[] args) {
        new Edge("Edge 01");
    }
}
