import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import Registry.ServiceRegistryInterface;
import utils.Logger;
import utils.ServiceRecord;

public class Client {
    private static Logger logger;
    private static ServiceRecord dcRecord;

    public static void main(String[] args) {
        logger = new Logger("Client");
        try {
            dcRecord = findDatacenter();
            if (dcRecord == null) {
                System.out.println("Não foi possível encontrar o DataCenter.");
                return;
            }
            
            logger.log("Conectado ao DataCenter em " + dcRecord.getHost() + ":" + dcRecord.getPort());

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
                        makeRequest("SUMMARY");
                        break;
                    case 2:
                        makeRequest("TEMPERATURE");
                        break;
                    case 3:
                        makeRequest("AIR_QUALITY");
                        break;
                    case 4:
                        makeRequest("NOISE");
                        break;
                    case 5:
                        makeRequest("UV");
                        break;
                    case 6:
                        makeRequest("STATUS");
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

    private static void makeRequest(String command) {
        try {
            Socket socket = new Socket(dcRecord.getHost(), dcRecord.getPort());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(command);
            String response = in.readLine();
            
            System.out.println("\n--- Resposta ---");
            System.out.println(response);

            socket.close();
        } catch (Exception e) {
            logger.log("Erro na requisição TCP: " + e.getMessage());
        }
    }

    private static ServiceRecord findDatacenter() {
        try {
            String registryHost = System.getenv("REGISTRY_HOST") != null ? System.getenv("REGISTRY_HOST") : "localhost";
            Registry registry = LocateRegistry.getRegistry(registryHost, 1099);
            ServiceRegistryInterface serviceRegistry = (ServiceRegistryInterface) registry.lookup("ServiceRegistry");
            
            ServiceRecord record = serviceRegistry.lookup("DataCenterClient");
            logger.log("DataCenter encontrado em " + record.getHost());

            return record;
        } catch (Exception e) {
            logger.log("Erro ao buscar DataCenter: " + e.getMessage());
            return null;
        }
    }
}
