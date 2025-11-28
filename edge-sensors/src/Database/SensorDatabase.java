package Database;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import Registry.ServiceRegistryInterface;
import utils.Logger;
import utils.SensorDTO;
import utils.ServiceRecord;

public class SensorDatabase extends UnicastRemoteObject implements DatabaseInterface {
    private List<SensorDTO> dataStore;
    private Logger logger;

    public SensorDatabase() throws RemoteException {
        super();
        dataStore = new ArrayList<>();
        logger = new Logger("SensorDatabase");
    }

    @Override
    public synchronized void insert(SensorDTO data) throws RemoteException {
        dataStore.add(data);
        logger.log("Dado inserido. Total: " + dataStore.size());
    }

    @Override
    public synchronized List<SensorDTO> getAll() throws RemoteException {
        return new ArrayList<>(dataStore);
    }

    private void registerService() throws RemoteException, NotBoundException, UnknownHostException {
        String registryHost = System.getenv("REGISTRY_HOST") != null ? System.getenv("REGISTRY_HOST") : "localhost";
        Registry registry = LocateRegistry.getRegistry(registryHost, 1099);
        ServiceRegistryInterface serviceRegistry = (ServiceRegistryInterface) registry.lookup("ServiceRegistry");
        
        String myHost = InetAddress.getLocalHost().getHostName();
        serviceRegistry.register("SensorDatabase", new ServiceRecord(myHost, 1101, "RMI"));
        logger.log("Registrado no ServiceRegistry como SensorDatabase em " + myHost + ":1101");
    }

    public static void main(String[] args) {
        try {
            SensorDatabase db = new SensorDatabase();
            Registry localRegistry = LocateRegistry.createRegistry(1101);
            localRegistry.rebind("SensorDatabase", db);
            db.logger.log("Banco de dados pronto na porta 1101...");
            
            db.registerService();
        } catch (Exception e) {
            System.err.println("Erro fatal no Database: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
