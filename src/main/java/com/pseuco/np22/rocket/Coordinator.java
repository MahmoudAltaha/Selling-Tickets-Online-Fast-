package com.pseuco.np22.rocket;

import java.util.List;

import com.pseuco.np22.Config;
import com.pseuco.np22.request.ServerId;

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
     * Returns the configuration of the system.
     * 
     * @return The configuration of the system.
     */
    public Config getConfig() {
        return this.config;
    }

    /**
     * Returns the database of the system.
     * 
     * @return The database of the system.
     */
    public Database getDatabase() {
        return this.database;
    }

    /**
     * Returns the mailbox of the estimator of the system.
     * 
     * @return The mailbox of the estimator of the system.
     */
    public Mailbox<Command<Estimator>> getEstimatorMailbox() {
        return this.estimator.getMailbox();
    }

    /**
     * Returns the mailbox of a specific server of the system.
     * 
     * @param serverId The id of the server.
     * @return The mailbox of the server with the given id.
     */
    public Mailbox<Command<Server>> getServerMailbox(ServerId serverId) {
        throw new RuntimeException("Not implemented!");
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
        throw new RuntimeException("Not implemented!");
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
        throw new RuntimeException("Not implemented!");
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
        throw new RuntimeException("Not implemented!");
    }

    /**
     * Scales the system to the given number of servers.
     * 
     * @param numServers The number of servers.
     * @return The number of servers.
     */
    public int scale(int numServers) {
        throw new RuntimeException("Not implemented!");
    }

    /**
     * Returns the number of active (non-terminating) servers.
     * 
     * @return The number of active (non-terminating) servers.
     */
    public int getNumOfServers() {
        throw new RuntimeException("Not implemented!");
    }

    /**
     * Returns a list of {@link ServerId} of the active servers.
     * 
     * @return A list of {@link ServerId} of the active servers.
     */
    public List<ServerId> getActiveServerIds() {
        throw new RuntimeException("Not implemented!");
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
        throw new RuntimeException("Not implemented!");
    }
}
