package Datacenter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.zip.CRC32;

import Database.DatabaseInterface;
import Registry.ServiceRegistryInterface;
import utils.Logger;
import utils.SensorDTO;
import utils.ServiceRecord;

public class DataCenter {
    private Boolean active = true;
    private Logger logger;
    private DatabaseInterface database;

    public DataCenter(String identifier) {
        logger = new Logger(identifier);
        startTCPServer();
        startClientServer();
    }

    private void connectToDatabase() throws RemoteException, NotBoundException {
        String registryHost = System.getenv("REGISTRY_HOST") != null ? System.getenv("REGISTRY_HOST") : "localhost";
        Registry registry = LocateRegistry.getRegistry(registryHost, 1099);
        ServiceRegistryInterface serviceRegistry = (ServiceRegistryInterface) registry.lookup("ServiceRegistry");
        ServiceRecord dbRecord = serviceRegistry.lookup("SensorDatabase");

        Registry dbRegistry = LocateRegistry.getRegistry(dbRecord.getHost(), dbRecord.getPort());
        database = (DatabaseInterface) dbRegistry.lookup("SensorDatabase");
        // logger.log("Conectado ao SensorDatabase em " + dbRecord.getHost());
    }

    private void insertWithRetry(SensorDTO dto) throws Exception {
        int attempts = 0;
        while (attempts < 5) {
            try {
                if (database == null) connectToDatabase();
                database.insert(dto);
                return;
            } catch (Exception e) {
                logger.log("Erro ao inserir dados (Tentativa " + (attempts+1) + ")");
                database = null;
                try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            }
            attempts++;
        }
        throw new Exception("FALHA CRÍTICA: Não foi possível inserir o dado após várias tentativas.");
    }

    private List<SensorDTO> getAllWithRetry() throws Exception {
        int attempts = 0;
        while (attempts < 5) {
            try {
                if (database == null) connectToDatabase();
                return database.getAll();
            } catch (Exception e) {
                logger.log("Erro ao buscar dados (Tentativa " + (attempts+1) + "): " + e.getMessage());
                database = null;
                try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            }
            attempts++;
        }
        throw new Exception("FALHA CRÍTICA: Não foi possível buscar dados após várias tentativas.");
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

                                CRC32 checksum = new CRC32();
                                checksum.update(dto.dataString().getBytes());

                                if (checksum.getValue() != dto.getChecksum()) {
                                    logger.log("ERRO: Checksum inválido recebido da Borda. Descartando dados.");
                                    continue;
                                }

                                insertWithRetry(dto);
                            }
                            logger.log("Dados recebidos e processados");
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

    private void startClientServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8082);
                logger.log("Servidor de Clientes ouvindo na porta 8082...");
                registerClientService();

                while (active) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handleClient(socket)).start();
                }
                serverSocket.close();
            } catch (IOException e) {
                logger.log("Erro no servidor de clientes: " + e.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String command = in.readLine();
            if (command != null) {
                String response;
                try {
                    switch (command) {
                        case "SUMMARY": response = getSummary(); break;
                        case "TEMPERATURE": response = getTemperatureStats(); break;
                        case "AIR_QUALITY": response = getAirQualityStatus(); break;
                        case "NOISE": response = getNoiseStats(); break;
                        case "UV": response = getUVStats(); break;
                        case "STATUS": response = getSystemStatus(); break;
                        default: response = "Comando desconhecido";
                    }
                } catch (Exception e) {
                    response = "Erro interno: " + e.getMessage();
                }
  
                CRC32 checksum = new CRC32();
                checksum.update(response.getBytes());
                long checksumValue = checksum.getValue();

                out.println(checksumValue + ":" + response);
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerClientService() {
        try {
            String registryHost = System.getenv("REGISTRY_HOST") != null ? System.getenv("REGISTRY_HOST") : "localhost";
            Registry registry = LocateRegistry.getRegistry(registryHost, 1099);
            ServiceRegistryInterface serviceRegistry = (ServiceRegistryInterface) registry.lookup("ServiceRegistry");
            
            String myHost = InetAddress.getLocalHost().getHostName();
            serviceRegistry.register("DataCenterClient", new ServiceRecord(myHost, 8082, "TCP"));
            logger.log("Registrado no ServiceRegistry como DataCenterClient em " + myHost + ":8082");
        } catch (Exception e) {
            logger.log("Erro ao registrar serviço de cliente: " + e.getMessage());
        }
    }

    public String getSummary() {
        logger.log("Gerando resumo geral");
        List<SensorDTO> data;
        try {
            data = getAllWithRetry();
        } catch (Exception e) {
            return "Erro ao acessar banco de dados: " + e.getMessage();
        }
        if (data.isEmpty()) {
            return "Nenhum dado disponível";
        }
        int count = data.size();
        double sumCo2 = 0, sumCo = 0, sumNo2 = 0, sumSo2 = 0, sumPm5 = 0, sumPm10 = 0, sumUmidade = 0, sumTemperatura = 0, sumRuido = 0, sumRadiacaoUV = 0;
        for (SensorDTO dto : data) {
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

    public String getTemperatureStats() {
        List<SensorDTO> data;
        try {
            data = getAllWithRetry();
        } catch (Exception e) {
            return "Erro ao acessar banco de dados: " + e.getMessage();
        }
        if (data.isEmpty()) return "Nenhum dado disponível";
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0;
        for (SensorDTO dto : data) {
            double temp = dto.getTemperatura();
            if (temp < min) min = temp;
            if (temp > max) max = temp;
            sum += temp;
        }
        return String.format("Estatísticas de Temperatura: Min=%.2f C, Max=%.2f C, Média=%.2f C", min, max, sum / data.size());
    }

    public String getAirQualityStatus() {
        List<SensorDTO> data;
        try {
            data = getAllWithRetry();
        } catch (Exception e) {
            return "Erro ao acessar banco de dados: " + e.getMessage();
        }
        if (data.isEmpty()) return "Nenhum dado disponível";
        double sumPm25 = 0;
        for (SensorDTO dto : data) {
            sumPm25 += dto.getPm5();
        }
        double avgPm25 = sumPm25 / data.size();
        String status;
        if (avgPm25 < 12) status = "Bom";
        else if (avgPm25 < 35) status = "Moderado";
        else if (avgPm25 < 55) status = "Insalubre para Grupos Sensíveis";
        else status = "Insalubre";
        return String.format("Qualidade do Ar (PM2.5): Status=%s, Média=%.2f ug/m3", status, avgPm25);
    }

    public String getNoiseStats() {
        List<SensorDTO> data;
        try {
            data = getAllWithRetry();
        } catch (Exception e) {
            return "Erro ao acessar banco de dados: " + e.getMessage();
        }
        if (data.isEmpty()) return "Nenhum dado disponível";
        double sum = 0;
        int loudCount = 0;
        for (SensorDTO dto : data) {
            sum += dto.getRuido();
            if (dto.getRuido() > 80) loudCount++;
        }
        return String.format("Estatísticas de Ruído: Média=%.2f dB, Eventos Altos (>80dB)=%d", sum / data.size(), loudCount);
    }

    public String getUVStats() {
        List<SensorDTO> data;
        try {
            data = getAllWithRetry();
        } catch (Exception e) {
            return "Erro ao acessar banco de dados: " + e.getMessage();
        }
        if (data.isEmpty()) return "Nenhum dado disponível";
        double max = 0;
        double sum = 0;
        int highUVCount = 0;
        for (SensorDTO dto : data) {
            double uv = dto.getRadiacaoUV();
            if (uv > max) max = uv;
            sum += uv;
            if (uv > 6) highUVCount++;
        }
        return String.format("Estatísticas UV: Max=%.2f, Média=%.2f, Eventos UV Alto (>6)=%d", max, sum / data.size(), highUVCount);
    }

    public String getSystemStatus() {
        List<SensorDTO> data;
        try {
            data = getAllWithRetry();
        } catch (Exception e) {
            return "Erro ao acessar banco de dados: " + e.getMessage();
        }
        if (data.isEmpty()) return "Status do Sistema: Nenhum dado recebido ainda.";
        long lastTime = data.get(data.size() - 1).getTimestamp();
        return String.format("Status do Sistema: Total de Leituras=%d, Último Recebimento=%tF %tT", data.size(), lastTime, lastTime);
    }
    
    public static void main(String[] args) {
        try {
            new DataCenter("DataCenter");
        } catch (Exception e) {
            System.err.println("Erro fatal na inicialização do DataCenter: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
