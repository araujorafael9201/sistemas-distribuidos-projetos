package Database;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import utils.SensorDTO;

public interface DatabaseInterface extends Remote {
    void insert(SensorDTO data) throws RemoteException;
    List<SensorDTO> getAll() throws RemoteException;
}
