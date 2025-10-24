package Calculator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class CalculatorImpl implements Runnable {
    private Socket client;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    public CalculatorImpl(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            inputStream = new DataInputStream(client.getInputStream());
            outputStream = new DataOutputStream(client.getOutputStream());
            
            boolean stop = false;

            while (!stop) {
                String request = inputStream.readUTF();
                String response;

                if (request.equalsIgnoreCase("end")) {
                    System.out.println("Closing connection with " + client.getInetAddress().getHostAddress());
                    stop = true;
                    response = "Closing connection...";
                } else {
                    System.out.println(String.format("Received calculation from %s: %s", client.getInetAddress().getHostAddress(), request));
                    String op = request.split(":")[0];
                    float n1 = Float.parseFloat(request.split(":")[1]);
                    float n2 = Float.parseFloat(request.split(":")[2]);

                    switch(op) {
                        case "+":
                            response = String.format("%.2f", n1 + n2);
                            break;
                        case "-":      
                            response = String.format("%.2f", n1 - n2);
                            break;
                        case "*":      
                            response = String.format("%.2f", n1 * n2);
                            break;
                        case "/":      
                            response = String.format("%.2f", n1 / n2);
                            break;
                        default:
                            response = "ERROR: Invalid operation " + op;
                            break;
                    }
                }
                System.out.println(String.format("Returning response to %s: %s", client.getInetAddress().getHostAddress(), response));
                outputStream.writeUTF(response);
            }

            inputStream.close();
            outputStream.close();
            client.close();
        } catch (IOException e) {
            System.err.println("Calculator error: " + e.getMessage());
        }
    }
}
