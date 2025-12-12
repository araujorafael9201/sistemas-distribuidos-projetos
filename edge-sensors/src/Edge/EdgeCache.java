package Edge;

import java.io.PrintWriter;
import java.net.Socket;

import utils.Logger;
import utils.SensorDTO;

public class EdgeCache {
    private SensorDTO[] items;
    private int size;
    private int head = 0;
    private Logger logger;

    public EdgeCache(int max_size) {
        if (max_size < 2) {
            throw new IllegalArgumentException("Cache não pode ser menor que 2");
        }
        size = max_size;
        items = new SensorDTO[max_size];
        logger = new Logger("Edge Cache");
    }

    public synchronized void add(SensorDTO data) {
        items[head] = data;
        head += 1;

        // logger.log("Tamanho atual do cache: " + head);
        if (head >= size) {
            logger.log("Tamanho máximo do cache excedido, iniciando sincronização...");
            
            SensorDTO[] dataToSend = items.clone(); // Clona dados a serem sincronizados para sincronizar em outra thread e liberar essa mais rápido
            head = 0;
            items = new SensorDTO[size];

            new Thread(() -> flushCache(dataToSend)).start();
        }
    }

    private void flushCache(SensorDTO[] dataToSend) {
        try {
            String host = System.getenv("DATACENTER_HOST") != null ? System.getenv("DATACENTER_HOST") : "localhost";
            Socket socket = new Socket(host, 8081);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            for (SensorDTO dto : dataToSend) {
                if (dto != null) {
                    out.println(dto.toString());
                }
            }
            out.close();
            socket.close();
            logger.log("Sincronização com servidor realizada com sucesso");
        } catch (Exception e) {
            logger.log("Erro na sincronização com o servidor. Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
