package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the central database for tickets.
 */
public class Database {
    /**
     * The tickets that are currently in the database.
     */
    private final List<Ticket> unallocated = new ArrayList<>();

    private Lock ticketLock; // lock for safe access/leave to the tickets
    private Condition isThereAcsesstoTicket; // Cheak if there is someone cheacking or changing tickets
    private int numAvailable; // the number of tickets available

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
        this.ticketLock = new ReentrantLock();
        this.isThereAcsesstoTicket = ticketLock.newCondition();
        this.numAvailable = coordinator.getConfig().getNumTickets();
    }

    /**
     * Returns the number of tickets available in the database.
     * 
     * @return The number of tickets available in the database.
     */
    public int getNumAvailable() {
        return numAvailable;
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
        List<Ticket> AllocatedTickets = new ArrayList<>();
        Ticket ticket = null;
        // if there is no tickets in Data Base
        if (numAvailable == 0) {
            return new ArrayList<>();
            // if there are tickets in Data Base as I asked
        } else if (numTickets <= numAvailable) {
            for (int i = 0; i < numTickets; i++) {
                ticket = unallocated.get(0);
                unallocated.remove(0);
                numAvailable--;
                AllocatedTickets.add(ticket);
            }
            return AllocatedTickets;
            // if there are tickets but not as I asked
        } else {
            for (int i = 0; i < numAvailable; i++) {
                ticket = unallocated.get(0);
                unallocated.remove(0);
                numAvailable--;
                AllocatedTickets.add(ticket);
            }
            return AllocatedTickets;
        }
    }

    /**
     * Deallocates previously allocated tickets.
     * 
     * @param tickets The tickets to return to the database.
     */
    public void deallocate(final Iterable<Ticket> tickets) {
        // add all tickets in unallocated List
        tickets.forEach((ticket) -> {
            unallocated.add(ticket);
            numAvailable++;
        });
    }

    /**
     * get condition
     */
    public Condition getIsFreeToTicket() {
        return isThereAcsesstoTicket;
    }

    /**
     * lock for safe access/leave to the tickets
     */
    public void TicketLock() {
        ticketLock.lock();
    }

    /**
     * unlock for safe access/leave to the tickets
     */
    public void TicketUnlock() {
        ticketLock.unlock();
    }
}
