package com.pseuco.np22.slug;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.pseuco.np22.Config;
import com.pseuco.np22.request.CustomerId;
import com.pseuco.np22.request.Request;
import com.pseuco.np22.request.RequestHandler;
import com.pseuco.np22.request.ServerId;

/**
 * A slow request handler processing requests sequentially.
 */
public class Slug implements RequestHandler {
    /**
     * The server id of this single server implementation.
     */
    private final ServerId serverId = ServerId.generate();

    /**
     * The stack of tickets still available.
     */
    private final Stack<Ticket> available;

    /**
     * The timeout of reservations in seconds.
     */
    private final int timeout;

    /**
     * Reservations made by customers.
     */
    private Map<CustomerId, Reservation> reservations = new HashMap<>();

    /**
     * Constructs a new slug.
     * 
     * @param config The options for the sales system.
     */
    public Slug(final Config config) {
        this.available = Ticket.generateStack(config.getNumTickets());
        this.timeout = config.getTimeout();
    }

    /**
     * Aborts and removes reservations with an expired timeout.
     */
    private void clearReservations() {
        this.reservations.values().removeIf(reservation -> {
            if (reservation.getAge() > this.timeout) {
                // Make the ticket available again.
                this.available.push(reservation.abort());
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public synchronized void handle(final Request request) {
        // We first need to clear reservations with an expired timeout. In your implementation,
        // you need to do something equivalently locally on each server.
        this.clearReservations();

        // Now, we can handle the request.
        switch (request.getKind()) {
            // This request is handled by the load balancer.
            case NUM_SERVERS: {
                switch (request.getMethod()) {
                    case GET: {
                        // In your implementation, you need to respond with the number of servers.
                        request.respondWithInt(1);
                        break;
                    }
                    case POST: {
                        final var numServers = request.readInt();
                        if (numServers.isEmpty()) {
                            // The client is supposed to provide a number of servers.
                            request.respondWithError("No number of servers provided!");
                        } else {
                            // In your implementation, you need to support this request for on-demand scaling. After
                            // scaling, you should respond with the number of servers.
                            request.respondWithError("Slug does not support on-demand scaling!");
                        }
                        break;
                    }
                }
                break;
            }
            // This request is handled by the load balancer.
            case GET_SERVERS: {
                // In your implementation, you need to respond with the ids of the active servers.
                final ServerId[] ids = { this.serverId };
                request.respondWithServerIds(Arrays.asList(ids));
                break;
            }

            // In your implementation, this request need to be redirected to a server.
            case NUM_AVAILABLE_TICKETS: {
                // This request requires us to respond with a server id.
                request.setServerId(this.serverId);
                // In your implementation, you respond with an approximation of the actual number.
                request.respondWithInt(this.available.size());
                break;
            }

            // In your implementation, this request need to be redirected to a server.
            case RESERVE_TICKET: {
                // This request requires us to respond with a server id.
                request.setServerId(this.serverId);

                final var customer = request.getCustomerId();
                if (this.reservations.containsKey(customer)) {
                    // We do not allow a customer to reserve more than a ticket at a time.
                    request.respondWithError("A ticket has already been reserved!");
                } else if (!this.available.isEmpty()) {
                    // Take a ticket from the stack of available tickets and reserve it.
                    final var ticket = this.available.pop();
                    this.reservations.put(customer, new Reservation(ticket));
                    // Respond with the id of the reserved ticket.
                    request.respondWithInt(ticket.getId());
                } else {
                    // Tell the client that no tickets are available.
                    request.respondWithSoldOut();
                }
                break;
            }
            // In your implementation, this request need to be redirected to a server.
            case ABORT_PURCHASE: {
                // This request requires us to respond with a server id.
                request.setServerId(this.serverId);

                final var customer = request.getCustomerId();
                if (!this.reservations.containsKey(customer)) {
                    // Without a reservation there is nothing to abort.
                    request.respondWithError("No ticket has been reserved!");
                } else {
                    final var reservation = this.reservations.get(customer);
                    final var ticketId = request.readInt();
                    if (ticketId.isEmpty()) {
                        // The client is supposed to provide a ticket id.
                        request.respondWithError("No ticket id provided!");
                    } else if (ticketId.get() == reservation.getTicketId()) {
                        // Abort the reservation and put the ticket back on the stack.
                        final var ticket = reservation.abort();
                        this.available.push(ticket);
                        this.reservations.remove(customer);
                        // Respond with the id of the formerly reserved ticket.
                        request.respondWithInt(ticket.getId());
                    } else {
                        // The id does not match the id of the reservation.
                        request.respondWithError("Invalid ticket id provided!");
                    }
                }
                break;
            }
            // In your implementation, this request need to be redirected to a server.
            case BUY_TICKET: {
                // This request requires us to respond with a server id.
                request.setServerId(this.serverId);

                final var customer = request.getCustomerId();
                if (!this.reservations.containsKey(customer)) {
                    // Without a reservation there is nothing to buy.
                    request.respondWithError("No ticket has been reserved!");
                } else {
                    final var reservation = this.reservations.get(customer);
                    final var ticketId = request.readInt();
                    if (ticketId.isEmpty()) {
                        // The client is supposed to provide a ticket id.
                        request.respondWithError("No ticket id provided!");
                    } else if (ticketId.get() == reservation.getTicketId()) {
                        // Sell the ticket to the customer.
                        final var ticket = reservation.sell();
                        this.reservations.remove(customer);
                        // Respond with the id of the sold ticket.
                        request.respondWithInt(ticket.getId());
                    } else {
                        // The id does not match the id of the reservation.
                        request.respondWithError("Invalid ticket id provided!");
                    }
                }
                break;
            }

            // Use this request for sending debug information of your choosing.
            case DEBUG: {
                request.respondWithString("This is 🐌.");
            }
        }
    }
}
