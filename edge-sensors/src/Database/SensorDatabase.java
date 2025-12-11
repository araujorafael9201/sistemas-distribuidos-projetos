package Database;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import Registry.ServiceRegistryInterface;
import utils.LogEntry;
import utils.Logger;
import utils.SensorDTO;
import utils.ServiceRecord;

public class SensorDatabase extends UnicastRemoteObject implements DatabaseInterface {
    private List<SensorDTO> dataStore;
    private List<LogEntry> changeLog;
    private Logger logger;
    private String myId;
    private int myPort;
    private Boolean leader;
    private DatabaseInterface leaderRef;
    private ServiceRecord currentLeader;
    private boolean running = true;
    
    // Known peers in the cluster
    private static final String[] PEER_HOSTS = {"database1", "database2", "database3"};
    private static final int PEER_PORT = 1101;

    public SensorDatabase(String id, int port) throws RemoteException {
        super();
        this.myId = id;
        this.myPort = port;
        this.dataStore = new ArrayList<>();
        this.changeLog = new ArrayList<>();
        this.logger = new Logger("DB-" + id);
        this.leader = false;
    }

    @Override
    public synchronized void insert(SensorDTO data) throws RemoteException {
        if (!leader) {
            throw new RemoteException("Can only insert on leader");
        }

        long index = changeLog.size() + 1;
        LogEntry entry = new LogEntry(index, "INSERT", data);
        
        List<String> targets = new ArrayList<>();
        for (String host : PEER_HOSTS) {
            if (!host.equals(myId)) {
                targets.add(host);
            }
        }

        if (targets.size() > 0) {
            replicateToPeer(entry, targets.get(0), true);
        }

        if (targets.size() > 1) {
            replicateToPeer(entry, targets.get(1), false);
        }

        applyEntry(entry);
        logger.log("Dado inserido e replicado. Index: " + index);
    }

    private void replicateToPeer(LogEntry entry, String host, boolean sync) {
        Runnable task = () -> {
            try {
                Registry remoteRegistry = LocateRegistry.getRegistry(host, PEER_PORT);
                DatabaseInterface peer = (DatabaseInterface) remoteRegistry.lookup("SensorDatabase");
                peer.replicate(entry);
            } catch (Exception e) {
                logger.log("Falha na replicação para " + host + ": " + e.getMessage());
            }
        };

        if (sync) {
            task.run();
        } else {
            new Thread(task).start();
        }
    }

    @Override
    public synchronized void replicate(LogEntry entry) throws RemoteException {
        if (entry.getIndex() > changeLog.size()) {
            applyEntry(entry);
            logger.log("Replicação recebida. Index: " + entry.getIndex());
        }
    }

    private void applyEntry(LogEntry entry) {
        changeLog.add(entry);
        if ("INSERT".equals(entry.getOperation())) {
            dataStore.add(entry.getData());
        }
    }

    @Override
    public synchronized List<LogEntry> sync(long lastKnownIndex) throws RemoteException {
        List<LogEntry> missing = new ArrayList<>();
        for (LogEntry entry : changeLog) {
            if (entry.getIndex() > lastKnownIndex) {
                missing.add(entry);
            }
        }
        return missing;
    }

    @Override
    public synchronized List<SensorDTO> getAll() throws RemoteException {
        return new ArrayList<>(dataStore);
    }

    @Override
    public long getLastLogIndex() throws RemoteException {
        return changeLog.size();
    }

    @Override
    public Boolean getLeader() throws RemoteException {
        return leader;
    }

    private void start() {
        new Thread(this::monitorRole).start();
    }

    private void monitorRole() {
        while (running) {
            try {
                if (leader) {
                    maintainLeadership();
                } else {
                    checkLeader();
                }
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkLeader() {
        try {
            if (currentLeader == null) {
                ServiceRegistryInterface registry = getServiceRegistry();
                try {
                    currentLeader = registry.lookup("SensorDatabase");
                } catch (Exception e) {
                }
                
                if (currentLeader == null) {
                    runElection(null);
                    return;
                }
            }

            try {
                Registry remoteRegistry = LocateRegistry.getRegistry(currentLeader.getHost(), currentLeader.getPort());
                leaderRef = (DatabaseInterface) remoteRegistry.lookup("SensorDatabase");
                leaderRef.getLeader(); // Ping
                
                List<LogEntry> updates = leaderRef.sync(changeLog.size());
                for (LogEntry entry : updates) {
                    applyEntry(entry);
                }
                
            } catch (RemoteException | java.rmi.NotBoundException re) {
                logger.log("Líder " + currentLeader.getHost() + " inacessível. Verificando registro...");
                
                ServiceRegistryInterface registry = getServiceRegistry();
                ServiceRecord registeredLeader = null;
                try {
                    registeredLeader = registry.lookup("SensorDatabase");
                } catch (Exception e) {
                }

                if (!registeredLeader.equals(currentLeader)) {
                    logger.log("Novo líder detectado: " + registeredLeader.getHost());
                    currentLeader = registeredLeader;
                } else {
                    logger.log("Líder morto ainda registrado. Tentando assumir...");
                    runElection(currentLeader);
                }
            }
        } catch (Exception e) {
            logger.log("Erro ao verificar líder: " + e.getMessage());
        }
    }

    private void runElection(ServiceRecord deadLeader) {
        try {
            ServiceRegistryInterface registry = getServiceRegistry();
            String myHost = InetAddress.getLocalHost().getHostName();
            ServiceRecord myRecord = new ServiceRecord(myHost, myPort, "RMI");
            
            boolean success = false;
            if (deadLeader == null) {
                try {
                    registry.register("SensorDatabase", myRecord);
                    success = true;
                } catch (java.rmi.AlreadyBoundException e) {
                }
            } else {
                success = registry.replace("SensorDatabase", deadLeader, myRecord);
            }

            if (success) {
                leader = true;
                currentLeader = myRecord;
                logger.log("ASSUMI A LIDERANÇA");
            } else {
                try {
                    currentLeader = registry.lookup("SensorDatabase");
                    leader = false;
                    logger.log("Seguindo novo líder: " + currentLeader.getHost());
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            logger.log("Erro na eleição: " + e.getMessage());
        }
    }

    private void maintainLeadership() {
        try {
             ServiceRegistryInterface registry = getServiceRegistry();
             String myHost = InetAddress.getLocalHost().getHostName();
             ServiceRecord myRecord = new ServiceRecord(myHost, myPort, "RMI");
             
             try {
                 registry.register("SensorDatabase", myRecord);
             } catch (java.rmi.AlreadyBoundException e) {
                 // Check if it's me
                 ServiceRecord current = registry.lookup("SensorDatabase");
                 if (!current.equals(myRecord)) {
                     logger.log("Outro líder registrado! Rebaixando...");
                     leader = false;
                     currentLeader = current;
                 }
             }
        } catch (Exception e) {
            // Registry error
        }
    }

    private ServiceRegistryInterface getServiceRegistry() throws Exception {
        String registryHost = System.getenv("REGISTRY_HOST") != null ? System.getenv("REGISTRY_HOST") : "localhost";
        Registry registry = LocateRegistry.getRegistry(registryHost, 1099);
        return (ServiceRegistryInterface) registry.lookup("ServiceRegistry");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java Database.SensorDatabase <id> <port>");
            System.exit(1);
        }

        String id = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            SensorDatabase db = new SensorDatabase(id, port);
            Registry localRegistry = LocateRegistry.createRegistry(port);
            localRegistry.rebind("SensorDatabase", db);
            
            db.logger.log("Database " + id + " iniciado na porta " + port);
            db.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
