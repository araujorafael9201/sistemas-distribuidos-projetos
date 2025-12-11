package Registry;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import utils.Logger;
import utils.ServiceRecord;
import java.rmi.RemoteException;
import java.rmi.AlreadyBoundException;

public class ServiceRegistry extends UnicastRemoteObject implements ServiceRegistryInterface {
    private Map<String, ServiceRecord> services;
    private Logger logger;

    public ServiceRegistry() throws RemoteException {
        super();
        services = new HashMap<>();
        logger = new Logger("ServiceRegistry");
    }

    @Override
    public void register(String serviceName, ServiceRecord record) throws RemoteException, AlreadyBoundException {
        synchronized (services) {
            if (!services.containsKey(serviceName)) {
                services.put(serviceName, record);
                logger.log("Serviço registrado: " + serviceName + " -> " + record);
            } else {
                throw new AlreadyBoundException("Serviço já registrado: " + serviceName);
            }
        }
    }

    @Override
    public boolean replace(String serviceName, ServiceRecord oldRecord, ServiceRecord newRecord) throws RemoteException {
        synchronized (services) {
            ServiceRecord current = services.get(serviceName);
            if (current != null && current.equals(oldRecord)) {
                services.put(serviceName, newRecord);
                logger.log("Serviço substituído (CAS): " + serviceName + " -> " + newRecord);
                return true;
            }
            return false;
        }
    }

    @Override
    public ServiceRecord lookup(String serviceName) throws RemoteException {
        ServiceRecord record = services.get(serviceName);
        if (record == null) {
            logger.log("Tentativa de busca falhou para: " + serviceName);
            throw new RemoteException("Serviço não encontrado: " + serviceName);
        }
        logger.log("Serviço consultado: " + serviceName + " -> " + record);
        return record;
    }

    public static void main(String[] args) {
        try {
            ServiceRegistry server = new ServiceRegistry();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ServiceRegistry", server);
            System.out.println("ServiceRegistry rodando na porta 1099...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
