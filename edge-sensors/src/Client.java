import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import Datacenter.DataCenterInterface;
import Registry.ServiceRegistryInterface;
import utils.Logger;
import utils.ServiceRecord;

public class Client {
    private static Logger logger;
    public static void main(String[] args) {
        logger = new Logger("Client");
        try {
            ServiceRecord dcRecord = findDatacenter();

            Registry dcRegistry = LocateRegistry.getRegistry(dcRecord.getHost(), dcRecord.getPort());
            DataCenterInterface dataCenter = (DataCenterInterface) dcRegistry.lookup("DataCenter");
            logger.log("Conectado ao DataCenter");

            Scanner scanner = new Scanner(System.in);
            System.out.println("Conectado ao DataCenter. Escolha uma opção:");

            while (true) {
                System.out.println("\nMenu:");
                System.out.println("1. Obter Resumo Geral");
                System.out.println("2. Estatísticas de Temperatura");
                System.out.println("3. Qualidade do Ar");
                System.out.println("4. Estatísticas de Ruído");
                System.out.println("5. Estatísticas de UV");
                System.out.println("6. Status do Sistema");
                System.out.println("0. Sair");
                System.out.print("Digite sua escolha: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        try {
                            System.out.println("\n--- Resumo ---");
                            System.out.println(dataCenter.getSummary());
                        } catch (Exception e) { logger.log("Erro: " + e.getMessage()); }
                        break;
                    case 2:
                        try {
                            System.out.println("\n--- Temperatura ---");
                            System.out.println(dataCenter.getTemperatureStats());
                        } catch (Exception e) { logger.log("Erro: " + e.getMessage()); }
                        break;
                    case 3:
                        try {
                            System.out.println("\n--- Qualidade do Ar ---");
                            System.out.println(dataCenter.getAirQualityStatus());
                        } catch (Exception e) { logger.log("Erro: " + e.getMessage()); }
                        break;
                    case 4:
                        try {
                            System.out.println("\n--- Ruído ---");
                            System.out.println(dataCenter.getNoiseStats());
                        } catch (Exception e) { logger.log("Erro: " + e.getMessage()); }
                        break;
                    case 5:
                        try {
                            System.out.println("\n--- Radiação UV ---");
                            System.out.println(dataCenter.getUVStats());
                        } catch (Exception e) { logger.log("Erro: " + e.getMessage()); }
                        break;
                    case 6:
                        try {
                            System.out.println("\n--- Status do Sistema ---");
                            System.out.println(dataCenter.getSystemStatus());
                        } catch (Exception e) { logger.log("Erro: " + e.getMessage()); }
                        break;
                    case 0:
                        System.out.println("Saindo...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Escolha inválida. Tente novamente.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao conectar ao servidor: " + e.getMessage());
            logger.log("Error ao conectar ao servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServiceRecord findDatacenter() {
        try {
            String registryHost = System.getenv("REGISTRY_HOST") != null ? System.getenv("REGISTRY_HOST") : "localhost";
            Registry registry = LocateRegistry.getRegistry(registryHost, 1099);
            ServiceRegistryInterface serviceRegistry = (ServiceRegistryInterface) registry.lookup("ServiceRegistry");
            
            ServiceRecord dcRecord = serviceRegistry.lookup("DataCenterRMI");
            logger.log("DataCenter encontrado em " + dcRecord.getHost());

            return dcRecord;
        } catch (Exception e) {
            System.out.println("Erro ao encontrar datacenter: " + e.getMessage());
            logger.log("Erro ao encontrar datacenter: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
