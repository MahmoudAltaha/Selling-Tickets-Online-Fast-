package com.pseuco.np22.slug;

/**
 * Represents a reservation of a ticket by a specific customer.
 */
public class Reservation {
    /**
     * The reserved ticket.
     */
    private final Ticket ticket;

    /**
     * The system time at which the ticket has been reserved.
     * 
     * This is necessary for determining when a reservation's timeout has expired.
     */
    private final long reservedAt;

    /**
     * Constructs a new reservation.
     * 
     * @param ticket The ticket that should be reserved.
     */
    public Reservation(final Ticket ticket) {
        ticket.reserve();
        this.ticket = ticket;
        this.reservedAt = System.currentTimeMillis();
    }

    /**
     * Returns the id of the reserved ticket.
     * 
     * @return The id of the reserved ticket.
     */
    public int getTicketId() {
        return this.ticket.getId();
    }

    /**
     * Returns the age of the reservation in seconds.
     * 
     * @return The age of the reservation in seconds.
     */
    public long getAge() {
        return (System.currentTimeMillis() - this.reservedAt) / 1000;
    }

    /**
     * Aborts the reservation and returns the ticket.
     * 
     * @return The ticket associated with the reservation.
     */
    public Ticket abort() {
        this.ticket.abort();
        return this.ticket;
    }

    /**
     * Marks the ticket as sold and returns it.
     * 
     * @return The ticket associated with the reservation.
     */
    public Ticket sell() {
        this.ticket.sell();
        return this.ticket;
    }
}