package Datacenter;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DataCenterInterface extends Remote {
    String getSummary() throws RemoteException;
    String getTemperatureStats() throws RemoteException;
    String getAirQualityStatus() throws RemoteException;
    String getNoiseStats() throws RemoteException;
    String getUVStats() throws RemoteException;
    String getSystemStatus() throws RemoteException;
}