package Datacenter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import utils.Logger;
import utils.SensorDTO;

public class DataCenter extends UnicastRemoteObject implements DataCenterInterface {
    private Boolean active = true;
    private Logger logger;
    private List<SensorDTO> database;

    public DataCenter(String identifier) throws RemoteException {
        logger = new Logger(identifier);
        database = new ArrayList<>();
        startTCPServer();
    }

    private void startTCPServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8081);
                logger.log("Ouvindo na porta 8081...");

                while (active) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String line;
                            while ((line = in.readLine()) != null) {
                                SensorDTO dto = SensorDTO.fromString(line);
                                synchronized (database) {
                                    database.add(dto);
                                }
                            }
                            logger.log("Dados recebidos e armazenados");
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }

                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public String getSummary() throws RemoteException {
        logger.log("Gerando resumo geral");
        synchronized (database) {
            if (database.isEmpty()) {
                return "No data available";
            }
            int count = database.size();
            double sumCo2 = 0, sumCo = 0, sumNo2 = 0, sumSo2 = 0, sumPm5 = 0, sumPm10 = 0, sumUmidade = 0, sumTemperatura = 0, sumRuido = 0, sumRadiacaoUV = 0;
            for (SensorDTO dto : database) {
                sumCo2 += dto.getCo2();
                sumCo += dto.getCo();
                sumNo2 += dto.getNo2();
                sumSo2 += dto.getSo2();
                sumPm5 += dto.getPm5();
                sumPm10 += dto.getPm10();
                sumUmidade += dto.getUmidade();
                sumTemperatura += dto.getTemperatura();
                sumRuido += dto.getRuido();
                sumRadiacaoUV += dto.getRadiacaoUV();
            }
            double avgCo2 = sumCo2 / count;
            double avgCo = sumCo / count;
            double avgNo2 = sumNo2 / count;
            double avgSo2 = sumSo2 / count;
            double avgPm5 = sumPm5 / count;
            double avgPm10 = sumPm10 / count;
            double avgUmidade = sumUmidade / count;
            double avgTemperatura = sumTemperatura / count;
            double avgRuido = sumRuido / count;
            double avgRadiacaoUV = sumRadiacaoUV / count;
            return "Summary: Count=" + count +
                   ", Avg CO2=" + avgCo2 +
                   ", Avg CO=" + avgCo +
                   ", Avg NO2=" + avgNo2 +
                   ", Avg SO2=" + avgSo2 +
                   ", Avg PM5=" + avgPm5 +
                   ", Avg PM10=" + avgPm10 +
                   ", Avg Umidade=" + avgUmidade +
                   ", Avg Temperatura=" + avgTemperatura +
                   ", Avg Ruido=" + avgRuido +
                   ", Avg RadiacaoUV=" + avgRadiacaoUV;
        }
    }

    public static void main(String[] args) {
        try {
            DataCenter dataCenter = new DataCenter("DataCenter");
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("DataCenter", dataCenter);
            dataCenter.logger.log("Servidor RMI pronto na porta 1099...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
