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
                return "Nenhum dado disponível";
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
            return "Resumo: Contagem=" + count +
                   ", Média CO2=" + avgCo2 +
                   ", Média CO=" + avgCo +
                   ", Média NO2=" + avgNo2 +
                   ", Média SO2=" + avgSo2 +
                   ", Média PM5=" + avgPm5 +
                   ", Média PM10=" + avgPm10 +
                   ", Média Umidade=" + avgUmidade +
                   ", Média Temperatura=" + avgTemperatura +
                   ", Média Ruído=" + avgRuido +
                   ", Média RadiaçãoUV=" + avgRadiacaoUV;
        }
    }

    @Override
    public String getTemperatureStats() throws RemoteException {
        synchronized (database) {
            if (database.isEmpty()) return "Nenhum dado disponível";
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            double sum = 0;
            for (SensorDTO dto : database) {
                double temp = dto.getTemperatura();
                if (temp < min) min = temp;
                if (temp > max) max = temp;
                sum += temp;
            }
            return String.format("Estatísticas de Temperatura: Min=%.2f C, Max=%.2f C, Média=%.2f C", min, max, sum / database.size());
        }
    }

    @Override
    public String getAirQualityStatus() throws RemoteException {
        synchronized (database) {
            if (database.isEmpty()) return "Nenhum dado disponível";
            double sumPm25 = 0;
            for (SensorDTO dto : database) {
                sumPm25 += dto.getPm5();
            }
            double avgPm25 = sumPm25 / database.size();
            String status;
            if (avgPm25 < 12) status = "Bom";
            else if (avgPm25 < 35) status = "Moderado";
            else if (avgPm25 < 55) status = "Insalubre para Grupos Sensíveis";
            else status = "Insalubre";
            return String.format("Qualidade do Ar (PM2.5): Status=%s, Média=%.2f ug/m3", status, avgPm25);
        }
    }

    @Override
    public String getNoiseStats() throws RemoteException {
        synchronized (database) {
            if (database.isEmpty()) return "Nenhum dado disponível";
            double sum = 0;
            int loudCount = 0;
            for (SensorDTO dto : database) {
                sum += dto.getRuido();
                if (dto.getRuido() > 80) loudCount++;
            }
            return String.format("Estatísticas de Ruído: Média=%.2f dB, Eventos Altos (>80dB)=%d", sum / database.size(), loudCount);
        }
    }

    @Override
    public String getUVStats() throws RemoteException {
        synchronized (database) {
            if (database.isEmpty()) return "Nenhum dado disponível";
            double max = 0;
            double sum = 0;
            int highUVCount = 0;
            for (SensorDTO dto : database) {
                double uv = dto.getRadiacaoUV();
                if (uv > max) max = uv;
                sum += uv;
                if (uv > 6) highUVCount++;
            }
            return String.format("Estatísticas UV: Max=%.2f, Média=%.2f, Eventos UV Alto (>6)=%d", max, sum / database.size(), highUVCount);
        }
    }

    @Override
    public String getSystemStatus() throws RemoteException {
        synchronized (database) {
            if (database.isEmpty()) return "Status do Sistema: Nenhum dado recebido ainda.";
            long lastTime = database.get(database.size() - 1).getTimestamp();
            return String.format("Status do Sistema: Total de Leituras=%d, Último Recebimento=%tF %tT", database.size(), lastTime, lastTime);
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
