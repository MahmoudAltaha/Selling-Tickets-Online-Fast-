package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the central database for tickets.
 */
public class Database {
    /**
     * The tickets that are currently in the database.
     */
    private final List<Ticket> unallocated = new ArrayList<>();

    /**
     * Constructs a new {@link Database}.
     * 
     * @param coordinator The {@link Coordinator} of the ticket sales system.
     */
    public Database(final Coordinator coordinator) {
        // Generate the necessary number of tickets.
        for (var id = 0; id < coordinator.getConfig().getNumTickets(); id++) {
            this.unallocated.add(new Ticket(id));
        }
    }

    /**
     * Returns the number of tickets available in the database.
     * 
     * @return The number of tickets available in the database.
     */
    public int getNumAvailable() {

        
        throw new RuntimeException("Not implemented!");
    }

    /**
     * <p>
     * Tries to allocate at most the given number of tickets.
     * </p>
     * 
     * <p>
     * 📌 Hint: Return an empty list in case the database has no tickets left.
     * </p>
     * 
     * @param numTickets The number of tickets to allocate.
     * @return A list of allocated tickets.
     */
    public List<Ticket> allocate(final int numTickets) {
        throw new RuntimeException("Not implemented!");
    }

    /**
     * Deallocates previously allocated tickets.
     * 
     * @param tickets The tickets to return to the database.
     */
    public void deallocate(final Iterable<Ticket> tickets) {
        throw new RuntimeException("Not implemented!");
    }
    
}
