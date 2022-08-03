package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import com.pseuco.np22.Config;
import com.pseuco.np22.request.ServerId;
import com.pseuco.np22.rocket.Server.MsgShutdown;

/**
 * <p>
 * The {@link Coordinator} orchestrates all the components of the system.
 * </p>
 */
public class Coordinator {
    /**
     * The configuration of the system.
     */
    private final Config config;

    /**
     * The database of the system.
     */
    protected final Database database;
    /**
     * The load balancer of the system.
     */
    protected final Balancer balancer;
    /**
     * The estimator of the system.
     */
    protected final Estimator estimator;

    /**
     * a Map that contains all active servers we have.
     */
    private HashMap<ServerId, Server> activeServers = new HashMap<ServerId, Server>();
    /**
     * a Map that contains all In-termination servers.
     */
    private HashMap<ServerId, Server> inTerminationServers = new HashMap<ServerId, Server>();

    /**
     * a Map that contains all servers we have.
     */
    private HashMap<ServerId, Server> allServers = new HashMap<ServerId, Server>();
    /**
     * a List that contains the ID of all active servers we have.
     */
    private List<ServerId> activeServersIDs = new ArrayList<ServerId>();
    /**
     * a List that contains the ID of all In-termination servers.
     */
    private List<ServerId> inTerminationServersIDs = new ArrayList<ServerId>();

    private List<ServerId> terminatedServerIDs = new ArrayList<ServerId>();
    /**
     * a List that contains the ID of all servers we have.
     */
    private List<ServerId> allServersIDs = new ArrayList<ServerId>();

    private ReentrantLock coordinatorLock = new ReentrantLock();

    /**
     * Constructs a new {@link Coordinator}.
     * 
     * @param config The configuration of the system.
     */
    public Coordinator(final Config config) {
        this.config = config;
        this.database = new Database(this);
        this.balancer = new Balancer(this);
        this.estimator = new Estimator(this);
    }

    /**
     * 
     * @return Map with all servers
     */
    public HashMap<ServerId, Server> getAllServers() {
        this.coordinatorLock.lock();
        try {
            return this.allServers;
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * Returns the configuration of the system.
     * 
     * @return The configuration of the system.
     */
    public Config getConfig() {
        this.coordinatorLock.lock();
        try {
            return this.config;
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * Returns the database of the system.
     * 
     * @return The database of the system.
     */
    public Database getDatabase() {
        this.coordinatorLock.lock();
        try {
            return this.database;
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * Returns the mailbox of the estimator of the system.
     * 
     * @return The mailbox of the estimator of the system.
     */
    public Mailbox<Command<Estimator>> getEstimatorMailbox() {
        this.coordinatorLock.lock();
        try {
            return this.estimator.getMailbox();
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * Returns the mailbox of a specific server of the system.
     * 
     * @param serverId The id of the server.
     * @return The mailbox of the server with the given id.
     */
    public Mailbox<Command<Server>> getServerMailbox(ServerId serverId) {
        this.coordinatorLock.lock();
        try {
            return allServers.get(serverId).getMailbox();
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * <p>
     * Picks a random server among the active (non-terminating) servers.
     * </p>
     * 
     * <p>
     * 📌 Hint: Useful for assigning (new) servers to clients.
     * </p>
     * 
     * @return The id of the randomly picked server.
     */
    public ServerId pickRandomServer() {
        this.coordinatorLock.lock();
        try {
            Random r = new Random();
            // Obtain a random number between [0 , (activeServersIDs.size()-1) ].
            int randomNumber = r.nextInt(activeServersIDs.size());
            // since the list also start from 0 , we don't have to add 1 to the result.
            return activeServersIDs.get(randomNumber);
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * <p>
     * Removes a server.
     * </p>
     * 
     * <p>
     * 📌 Hint: Should be called after a server has terminated to completely remove it
     * from the system.
     * </p>
     * 
     * @param serverId The id of the server to remove.
     */
    public void removeServer(ServerId serverId) {

        Server removedServer = activeServers.remove(serverId); // remove the server from the activeServers Map
        activeServersIDs.remove(serverId); // remove the id from the activeServerIds List
        inTerminationServers.put(serverId, removedServer); // add the removed Server to the terminatedServer Map
        inTerminationServersIDs.add(serverId); // add the removed Server to the terminatedServer List
        // send msgShutdown to the server
        Command<Server> mShutdown = new MsgShutdown();
        removedServer.getMailbox().sendHighPriority(mShutdown);

    }

    /**
     * <p>
     * Spins up a new server and returns its id.
     * </p>
     * 
     * <p>
     * 📌 Hint: Use this to start new servers for on-demand scaling.
     * </p>
     * 
     * @return The id of the new server.
     */
    public ServerId createServer() {

        ServerId id = ServerId.generate(); // create new serverID
        Server newServer = new Server(id, this); // create new Server with the generated id
        // add the new server to the HashMap/List of active server/ServerID
        activeServers.put(id, newServer);
        allServers.put(id, newServer);
        activeServersIDs.add(id);
        allServersIDs.add(id);
        // start the server as a thread
        new Thread(newServer).start();
        return id; // return the id of the created server

    }

    /**
     * Scales the system to the given number of servers.
     * 
     * @param numServers The number of servers.
     * @return The number of servers.
     */
    public int scale(int numServers) {
        this.coordinatorLock.lock();
        try {
            /*
             * if there are no servers yet created (beginn fo the system) the create the servers
             * according to the giving number.
             */
            if (activeServers.isEmpty()) {
                for (int i = 0; i < numServers; i++) {
                    createServer();
                }
                /*
                 * if the number of wished servers are bigger than the current active servers we have then
                 * create the servers we still need
                 */
            } else if (numServers > activeServers.size()) {
                int numOfServersToCreate = numServers - activeServers.size();
                for (int i = 0; i < numOfServersToCreate; i++) {
                    createServer();
                }
            }
            /*
             * if the number of wished servers are smaller than the current active servers we have
             * then remove the servers we do not want
             * for easy work we do not pick randomly we just remove the first server we get from the
             * list of active servers.
             */
            else if (numServers < activeServers.size()) {
                int numOfServersToRemove = activeServers.size() - numServers;
                for (int i = 0; i < numOfServersToRemove; i++) {
                    removeServer(activeServersIDs.remove(0));
                }
            }
            return this.getNumOfServers();
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * Returns the number of active (non-terminating) servers.
     * 
     * @return The number of active (non-terminating) servers.
     */
    public int getNumOfServers() {
        this.coordinatorLock.lock();
        try {
            return activeServers.size();
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * Returns a list of {@link ServerId} of the active servers.
     * 
     * @return A list of {@link ServerId} of the active servers.
     */
    public List<ServerId> getActiveServerIds() {
        this.coordinatorLock.lock();
        try {
            List<ServerId> listOfActiveServerIds = new ArrayList<>();
            listOfActiveServerIds.addAll(activeServersIDs);
            return listOfActiveServerIds;
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * Returns a list of {@link ServerId} servers that the in proces of termination.
     * 
     * @return A list of {@link ServerId} servers that the in proces of termination
     */
    public List<ServerId> getinTerminationServersIDs() {
        this.coordinatorLock.lock();
        try {
            List<ServerId> listOfInTerminationServerIds = new ArrayList<>();
            listOfInTerminationServerIds.addAll(activeServersIDs);
            return listOfInTerminationServerIds;
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * 
     * remove the server from the list of inTerminationServersIDs
     * In this way, when the Balancer check if this List have a associated Server,
     * then send the request to this server, other ways send the request to other active
     * Server
     * 
     * @param id
     */
    public void removefromInTermination(ServerId id) {
        this.coordinatorLock.lock();
        try {
            this.inTerminationServersIDs.remove(id);
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    /**
     * <p>
     * Returns a list of {@link ServerId} of all servers.
     * </p>
     * 
     * <p>
     * 📌 Hint: Use this in the {@link Estimator} to send messages to all servers (and not
     * just those which are still active).
     * </p>
     * 
     * @return A list of {@link ServerId} of the all servers.
     */
    public List<ServerId> getAllServerIds() {
        this.coordinatorLock.lock();
        try {
            List<ServerId> listOfAllServerIds = new ArrayList<>();
            listOfAllServerIds.addAll(activeServersIDs);
            return listOfAllServerIds;
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    public List<ServerId> getTerminatedServerIds() {
        this.coordinatorLock.lock();
        try {
            List<ServerId> listOfTerminatedServerIds = new ArrayList<>();
            listOfTerminatedServerIds.addAll(activeServersIDs);
            return listOfTerminatedServerIds;
        } finally {
            this.coordinatorLock.unlock();
        }
    }

    public void addToTerminatedServerIds(ServerId id) {
        this.coordinatorLock.lock();
        try {
            terminatedServerIDs.add(id);
        } finally {
            this.coordinatorLock.unlock();
        }
    }
}
