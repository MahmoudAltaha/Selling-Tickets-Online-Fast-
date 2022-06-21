package com.pseuco.np22.slug;

import java.util.Stack;

/**
 * Represents a ticket and its state transitions.
 */
public class Ticket {
    /**
     * Represents the state of a ticket.
     */
    public static enum State {
        /**
         * The ticket is <em>available</em>, i.e., it has neither been reserved nor sold.
         */
        AVAILABLE,
        /**
         * The ticket has been <em>reserved</em> by a customer.
         */
        RESERVED,
        /**
         * The ticket has been <em>sold</em> to a customer.
         */
        SOLD;
    }

    /**
     * Generates a stack of <em>numTickets</em> tickets.
     * 
     * @param numTickets The amount of tickets to generate.
     * @return A stack of tickets.
     */
    public static Stack<Ticket> generateStack(final int numTickets) {
        final var tickets = new Stack<Ticket>();
        for (var id = 0; id < numTickets; id++) {
            tickets.add(new Ticket(id));
        }
        return tickets;
    }

    /**
     * The id of the ticket.
     */
    private final int id;

    /**
     * The state of the ticket.
     */
    private State state = State.AVAILABLE;

    /**
     * Constructs a new ticket with the provided <em>id</em>.
     * 
     * @param id The id of the ticket.
     */
    public Ticket(final int id) {
        this.id = id;
    }

    /**
     * Returns the id of the ticket.
     * 
     * @return The id of the ticket.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Returns the state of the ticket.
     * 
     * @return The state of the ticket.
     */
    public State getState() {
        return this.state;
    }

    /**
     * <em>Reserves</em> the ticket.
     */
    public void reserve() {
        assert this.state == State.AVAILABLE : "Ticket is not available!";
        this.state = State.RESERVED;
    }

    /**
     * <em>Aborts</em> the reservation of the ticket.
     */
    public void abort() {
        assert this.state == State.RESERVED : "Ticket is not reserved!";
        this.state = State.AVAILABLE;
    }

    /**
     * <em>Sells</em> the ticket.
     */
    public void sell() {
        assert this.state == State.RESERVED : "Ticket is not reserved!";
        this.state = State.SOLD;
    }
}
