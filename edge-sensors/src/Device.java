import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.zip.CRC32;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import Registry.ServiceRegistryInterface;
import utils.ServiceRecord;

import utils.Logger;
import utils.SensorDTO;

public class Device {
    public String identifier;
    public Boolean active;
    private Logger logger;

    public Device(String id) {
        active = true;
        identifier = id;
        logger = new Logger(identifier);
        start();
    }

    private ServiceRecord findEdge() {
        try {
            logger.log("Buscando endereço da borda");
            String registryHost = System.getenv("REGISTRY_HOST") != null ? System.getenv("REGISTRY_HOST") : "localhost";
            Registry registry = LocateRegistry.getRegistry(registryHost, 1099);
            ServiceRegistryInterface serviceRegistry = (ServiceRegistryInterface) registry.lookup("ServiceRegistry");
            ServiceRecord edgeRecord = serviceRegistry.lookup("EdgeService");

            String edgeLocation = edgeRecord.getHost();
            int edgePort = edgeRecord.getPort();
            logger.log("Edge encontrado em " + edgeLocation + ":" + edgePort);

            return edgeRecord;
        } catch (Exception e) {
            logger.log("Erro ao buscar borda: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void start() {
        try {
            ServiceRecord edge = findEdge();

            if (edge == null) {
                throw new Exception("Não foi possível localizar a borda");
            }

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

                CRC32 checksum = new CRC32();
                checksum.update(data.dataString().getBytes());
                data.setChecksum(checksum.getValue());

                byte[] buffer = data.toString().getBytes();
                
                DatagramPacket edgePacket = new DatagramPacket(buffer, buffer.length, new InetSocketAddress(edge.getHost(), edge.getPort()));
                edgeSocket.send(edgePacket);
                logger.log("Dados enviados para a borda em " + edge.getHost());
                
                long sleepTime = 2000 + random.nextInt(1000);
                Thread.sleep(sleepTime);
            }

            edgeSocket.close();
        } catch (Exception e) {
            logger.log("Erro no dispositivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String id = System.getenv("DEVICE_ID") != null ? System.getenv("DEVICE_ID") : "Dispositivo 1";
        new Device(id);
    }
}
