import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

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
            while (active) {

                double co2 = 0.01; 
                double co = 0.01;
                double no2 = 0.01;
                double so2 = 0.01;
                double pm5 = 0.01;
                double pm10 = 0.01;
                double umidade = 0.01;
                double temperatura = 0.01;
                double ruido = 0.01;
                double radiacaoUV = 0.01;

                SensorDTO data = new SensorDTO(co2, co, no2, so2, pm5, pm10, umidade, temperatura, ruido, radiacaoUV);
                byte[] buffer = data.toString().getBytes();

                DatagramPacket edgePacket = new DatagramPacket(buffer, buffer.length, new InetSocketAddress(edgeLocation, edgePort));
                edgeSocket.send(edgePacket);
                logger.log("Dados enviados para a borda em " + edgeLocation);
                Thread.sleep(2000);
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
