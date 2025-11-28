package Datacenter;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DataCenterInterface extends Remote {
    String getSummary() throws RemoteException;
}