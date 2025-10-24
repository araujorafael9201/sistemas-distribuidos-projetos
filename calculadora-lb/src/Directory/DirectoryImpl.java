package Directory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class DirectoryImpl implements Runnable {
    private Socket client;
    private String addr;

    public DirectoryImpl(Socket client, String addr) {
        this.client = client;
        this.addr = addr;
    }

    @Override
    public void run() {
        try {
            DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
            System.out.println(String.format("Returning response to %s: %s", client.getInetAddress().getHostAddress(), addr));
            outputStream.writeUTF(addr);
            client.close();
        } catch (IOException e) {
            System.err.println("Directory error: " + e.getMessage());
        }
    }
}

