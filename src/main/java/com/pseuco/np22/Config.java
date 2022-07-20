package com.pseuco.np22;

/**
 * Configuration of the ticket sales system.
 */
public class Config {
    /**
     * The number of tickets initially available.
     */
    private final int numTickets;
    /**
     * The timeout of reservations in seconds.
     */
    private final int timeout;
    /**
     * The number of initial servers.
     */
    private final int initialServers;

    /**
     * Constructs a new instance from the provided parameters.
     * 
     * @param numTickets The number of tickets initially available.
     * @param timeout    The timeout of reservations in seconds.
     */
    protected Config(final int numTickets, final int timeout) {
        this.numTickets = numTickets;
        this.timeout = timeout;
        // We just set this to two for now.
        this.initialServers = 1;
    }

    /**
     * Returns the number of tickets initially available.
     * 
     * @return The number of tickets initially available.
     */
    public int getNumTickets() {
        return this.numTickets;
    }

    /**
     * Returns the timeout of reservations in seconds.
     * 
     * @return The timeout of reservations in seconds.
     */
    public int getTimeout() {
        return this.timeout;
    }

    /**
     * Returns the number of initial servers.
     * 
     * @return The number of initial servers.
     */
    public int getInitialServers() {
        return this.initialServers;
    }
}
