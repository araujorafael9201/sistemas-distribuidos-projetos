package Registry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import utils.ServiceRecord;

public interface ServiceRegistryInterface extends Remote {
    void register(String serviceName, ServiceRecord record) throws RemoteException;
    ServiceRecord lookup(String serviceName) throws RemoteException;
}
