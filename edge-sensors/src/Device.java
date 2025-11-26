import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;

import utils.Logger;
import utils.SensorDTO;

public class Device {
    public String identifier;
    public String edgeLocation;
    public int edgePort;
    public Boolean active;
    private Logger logger;

    public Device(String id) {
        active = true;
        identifier = id;
        edgeLocation = getEdgeLocation();
        edgePort = 8080;
        logger = new Logger(identifier);
        start();
    }

    private void start() {
        try {
            DatagramSocket edgeSocket = new DatagramSocket();
            Random random = new Random();

            while (active) {
                double co2 = 350 + random.nextDouble() * (1000 - 350); // 350-1000 ppm
                double co = 0.1 + random.nextDouble() * (10 - 0.1);    // 0.1-10 ppm
                double no2 = 0.01 + random.nextDouble() * (0.1 - 0.01); // 0.01-0.1 ppm
                double so2 = 0.001 + random.nextDouble() * (0.05 - 0.001); // 0.001-0.05 ppm
                double pm5 = 5 + random.nextDouble() * (100 - 5);      // 5-100 µg/m³
                double pm10 = 10 + random.nextDouble() * (150 - 10);   // 10-150 µg/m³
                double umidade = 20 + random.nextDouble() * (90 - 20); // 20-90 %
                double temperatura = 15 + random.nextDouble() * (35 - 15); // 15-35 °C
                double ruido = 40 + random.nextDouble() * (90 - 40);   // 40-90 dB
                double radiacaoUV = 1 + random.nextDouble() * 10;      // 1-11 Index
                long timestamp = System.currentTimeMillis();

                SensorDTO data = new SensorDTO(co2, co, no2, so2, pm5, pm10, umidade, temperatura, ruido, radiacaoUV, timestamp);
                byte[] buffer = data.toString().getBytes();

                DatagramPacket edgePacket = new DatagramPacket(buffer, buffer.length, new InetSocketAddress(edgeLocation, edgePort));
                edgeSocket.send(edgePacket);
                logger.log("Dados enviados para a borda em " + edgeLocation);
                
                long sleepTime = 2000 + random.nextInt(1000);
                Thread.sleep(sleepTime);
            }

            edgeSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Utiliza RMI para localizar o servidor de borda
     */
    private String getEdgeLocation() {
        return "127.0.0.1";
    }

    public static void main(String[] args) {
        new Device("Dispositivo 1");
    }
}
