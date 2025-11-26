import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import utils.Logger;

public class Client {
    private static Logger logger;

    public static void main(String[] args) {
        logger = new Logger("Client");
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            DataCenterInterface dataCenter = (DataCenterInterface) registry.lookup("DataCenter");
            logger.log("Conectado ao DataCenter");

            Scanner scanner = new Scanner(System.in);
            System.out.println("Conectado ao DataCenter. Escolha uma opção:");

            while (true) {
                System.out.println("\nMenu:");
                System.out.println("1. Obter Resumo Geral");
                System.out.println("0. Sair");
                System.out.print("Digite sua escolha: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        try {
                            String summary = dataCenter.getSummary();
                            System.out.println("\n--- Resumo ---");
                            System.out.println(summary);
                        } catch (Exception e) {
                            logger.log("Erro ao obter resumo: " + e.getMessage());
                            e.printStackTrace();
                        }
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
}
