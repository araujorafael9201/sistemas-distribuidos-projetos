package Client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientImpl implements Runnable {
    private Socket directory;
    private Scanner scanner;

    public ClientImpl(Socket directory) {
        this.directory = directory;
        this.scanner = new Scanner(System.in);
    }

    public void getResultFromCalculator(Socket calculator) {
        try {
            DataInputStream inputStream = new DataInputStream(calculator.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(calculator.getOutputStream());

            boolean stop = false;
            while (!stop) {
                System.out.print("Query: ");
                String request = scanner.nextLine();

                if (request.equalsIgnoreCase("end")) {
                    stop = true;
                }
                outputStream.writeUTF(request);
                String response = inputStream.readUTF();
                System.out.println("Received response from calculator: " + response);
            }

            System.out.println("Closing connection...");

            inputStream.close();
            outputStream.close();
            scanner.close();
            calculator.close();
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Get calculator address from directory
            DataInputStream inputStream = new DataInputStream(directory.getInputStream());
            String calculatorAddr = inputStream.readUTF();
            // Connect to calculator
            System.out.println("Got calculator address: " + calculatorAddr);
            Socket calculator = new Socket(calculatorAddr, 8888);
            System.out.println("Succesfully connected to calculator on: " + calculatorAddr);
            // Perform operation
            getResultFromCalculator(calculator);
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}


