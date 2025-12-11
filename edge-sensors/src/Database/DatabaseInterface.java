package Database;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import utils.LogEntry;
import utils.SensorDTO;

public interface DatabaseInterface extends Remote {
    void insert(SensorDTO data) throws RemoteException;
    List<SensorDTO> getAll() throws RemoteException;
    
    void replicate(LogEntry entry) throws RemoteException;
    List<LogEntry> sync(long lastKnownIndex) throws RemoteException;
    long getLastLogIndex() throws RemoteException;
    Boolean getLeader() throws RemoteException;
}
