package Registry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.AlreadyBoundException;
import utils.ServiceRecord;

public interface ServiceRegistryInterface extends Remote {
    void register(String serviceName, ServiceRecord record) throws RemoteException, AlreadyBoundException;
    boolean replace(String serviceName, ServiceRecord oldRecord, ServiceRecord newRecord) throws RemoteException;
    ServiceRecord lookup(String serviceName) throws RemoteException;
}
