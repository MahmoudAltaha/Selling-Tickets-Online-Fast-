package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;

import com.pseuco.np22.request.Request;
import com.pseuco.np22.request.CustomerId;
import com.pseuco.np22.request.ServerId;
import com.pseuco.np22.rocket.Estimator.MsgAvailableServer;
import com.pseuco.np22.rocket.Ticket.State;

/**
 * Implements the server.
 */
public class Server implements Runnable {
    /**
     * The server's id.
     */
    protected final ServerId id;

    /**
     * The {@link Coordinator} of the ticket sales system.
     */
    protected final Coordinator coordinator;

    /**
     * The mailbox of the {@link Estimator}.
     */
    private final Mailbox<Command<Server>> mailbox = new Mailbox<>();

    /*
     * Useful for the execute of requests from balancer , if the coordinator has sent a shut
     * down msg to the server then
     * the server itself would change his active status to "INTERMIATION" in the execute
     * methode of
     * msgShutdown. In this case the server must not accept
     * new request for new reservations. Therfore, he checks his state in the
     * msgprocessrequest
     * and acting according to his state.
     */
    private ServerState state = ServerState.ACTIVE;

    public static enum ServerState {
        /**
         * The server is active and can process all kind of requests.
         */
        ACTIVE,
        /**
         * The servr has resieved a shutdown msg and started terination steps
         */
        INTERMINATION,
        /**
         * The server is terminated .
         */
        TERMINATED;
    }

    private ReentrantLock serverStateLock = new ReentrantLock();

    /**
     * Reservations made by customers. //TODO: I replace the List by Map
     */
    private Map<CustomerId, Reservation> reservations = new HashMap<>();

    /**
     * List of allocated tickets from DB. //TODO think about allocateing tickets in case
     * Server/Or DB have no tickets left.
     */
    private List<Ticket> allocatedTickets = new ArrayList<Ticket>();

    /**
     * the Tickets in this List are all sold , they should not be in allocatedTickets list
     * anymore (still not sure if we need it, we will see)
     */
    private List<Ticket> soldTickets = new ArrayList<Ticket>();

    /**
     * number of available tickets "non-reserved"
     */
    private int NonReservedTickets;

    /**
     * Current ticket estimation from estimator
     */
    private int currentTicketEstimation = 0;

    /**
     * Constructs a new {@link Server}.
     * 
     * @param id          The id of the server.
     * @param coordinator The {@link Coordinator} of the ticket sales system.
     */
    public Server(ServerId id, Coordinator coordinator) {
        this.id = id;
        this.coordinator = coordinator;
        List<Ticket> tikets = this.coordinator.getDatabase().allocate(10);
        if (!tikets.isEmpty()) {
            for (int i = 0; i < tikets.size(); i++) {
                this.getAllocatedTickets().add(tikets.get(0));
            }
        }
    }

    /**
     * Returns the {@link Mailbox} of the server.
     * 
     * @return The {@link Mailbox} of the server.
     */
    public Mailbox<Command<Server>> getMailbox() {
        return this.mailbox;
    }

    /**
     * set ticket estimation
     */
    private void setCurrentTicketEstimation(int i) {
        this.currentTicketEstimation = i;
    }

    /**
     * Methode returns the number of allocated tickets "All Tickets"
     */
    private int getNumAllocatedTickets() {
        return allocatedTickets.size();
    }

    /**
     * 
     * @return list with allocated tickets
     */
    private List<Ticket> getAllocatedTickets() {
        return this.allocatedTickets;
    }

    /**
     * 
     * @return number of non-Reserved Tickets
     */
    private int getNonReservedTickets() {
        return this.allocatedTickets.size();
    }

    /**
     * Return the status of the server (active or not)
     * 
     * @return The {@link active}
     */
    public boolean isActive() {
        serverStateLock.lock();
        try {
            return (this.state.equals(ServerState.ACTIVE));
        } finally {
            serverStateLock.unlock();
        }

    }

    /**
     * 
     * @return
     */
    public boolean isTerminated() {
        serverStateLock.lock();
        try {
            return this.state.equals(ServerState.TERMINATED);
        } finally {
            serverStateLock.unlock();
        }

    }

    /**
     * set the status of the Server to non active
     */
    private void deactivateServer() {
        serverStateLock.lock();
        try {
            this.state = ServerState.INTERMINATION;
        } finally {
            serverStateLock.unlock();
        }

    }

    /**
     * 
     */
    private void terminateServer() {
        serverStateLock.lock();
        try {
            this.state = ServerState.TERMINATED;
        } finally {
            serverStateLock.unlock();
        }
    }

    /**
     * Aborts and removes reservations with an expired timeout.
     */
    private void clearReservations() {
        this.reservations.values().removeIf(reservation -> {
            if (reservation.getAge() > this.coordinator.getConfig().getTimeout()) {
                // Make the ticket available again.
                this.allocatedTickets.add(reservation.abort());
                return true;
            } else {
                return false;
            }
        });

    }

    @Override
    public void run() {
        /*
         * TODO: Implement the server as described in the project description. The
         * server will process the messages sent to its mailbox.
         */

        try {
            boolean keepHandlingMsg = true;

            while (keepHandlingMsg) {
                Command<Server> message = getMailbox().recv();
                // make sure that the mailbox did not returns a null msg to avoid calling execute on null
                if (message != null) {
                    message.execute(this);
                }
                if (!isActive() && getMailbox().isEmpty() && this.reservations.isEmpty()) {
                    keepHandlingMsg = false;
                }
            }
            // the server was in Terminating state and he has finished handling existing requests so
            // he could now terminate
            this.terminateServer();
            Thread.currentThread().interrupt();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * A message containing a {@link Request} that should be processed by the server.
     */
    public static class MsgProcessRequest implements Command<Server> {
        /**
         * The request that should be processed.
         */
        private final Request request;

        /**
         * Constructs a new {@link MsgProcessRequest} message.
         * 
         * @param request The {@link Request} to process.
         */
        public MsgProcessRequest(Request request) {
            this.request = request;
        }

        @Override
        public void execute(Server obj) {
            /*
             * 📌 Hint: Use the 🐌 implementation as a basis.
             */

            // note: this implementaion is very identical with the 🐌 implementation

            obj.clearReservations();
            switch (request.getKind()) {

                case NUM_AVAILABLE_TICKETS: {
                    // respond with an approximation of the actual number.
                    int currentTicketEstimation = obj.currentTicketEstimation;
                    request.respondWithInt(currentTicketEstimation);

                    break;
                }
                case RESERVE_TICKET: {

                    final var customer = request.getCustomerId();
                    if (obj.reservations.containsKey(customer)) {
                        // We do not allow a customer to reserve more than a ticket at a time.
                        request.respondWithError("A ticket has already been reserved!");

                    } else if (obj.getNumAllocatedTickets() > 0) {
                        // Take a ticket from the list of allocatedTickets tickets and reserve it.
                        final var ticket = obj.allocatedTickets.get(0);
                        obj.allocatedTickets.remove(0);
                        obj.reservations.put(customer, new Reservation(ticket));
                        // Respond with the id of the reserved ticket.
                        request.respondWithInt(ticket.getId());
                    } else
                        // TODO : make "if-condiation" for list obj.coordinator.getDatabase().allocate(10);
                        // Tell the client that no tickets are available,
                        // see the load balancer
                        request.respondWithSoldOut();

                    break;
                } // TODO :we have to get the abort ticket back to data base direct, only if ther server
                  // nonactive state
                case ABORT_PURCHASE: {
                    final var customer = request.getCustomerId();
                    if (!obj.reservations.containsKey(customer)) {
                        // Without a reservation there is nothing to abort.
                        request.respondWithError("No ticket has been reserved!");
                    } else {
                        final var reservation = obj.reservations.get(customer);

                        final var ticketId = request.readInt();
                        if (ticketId.isEmpty()) {
                            // The client is supposed to provide a ticket id.
                            request.respondWithError("No ticket id provided!");
                        } else if (ticketId.get() == reservation.getTicketId()) {
                            // Abort the reservation and put the ticket back on the allocatedTickets.
                            final var ticket = reservation.abort();
                            obj.allocatedTickets.add(ticket);
                            obj.reservations.remove(customer);
                            // Respond with the id of the formerly reserved ticket.
                            request.respondWithInt(ticket.getId());

                        } else {
                            // The id does not match the id of the reservation.
                            request.respondWithError("Invalid ticket id provided!");
                        }

                    }
                    break;
                }
                case BUY_TICKET: {
                    final var customer = request.getCustomerId();
                    if (!obj.reservations.containsKey(customer)) {
                        // Without a reservation there is nothing to buy.
                        request.respondWithError("No ticket has been reserved!");
                    } else {
                        final var reservation = obj.reservations.get(customer);
                        final var ticketId = request.readInt();
                        if (ticketId.isEmpty()) {
                            // The client is supposed to provide a ticket id.
                            request.respondWithError("No ticket id provided!");
                        } else if (ticketId.get() == reservation.getTicketId()) {
                            // Sell the ticket to the customer.
                            final var ticket = reservation.sell();
                            obj.reservations.remove(customer);
                            // Respond with the id of the sold ticket.
                            request.respondWithInt(ticket.getId());

                        } else {
                            // The id does not match the id of the reservation.
                            request.respondWithError("Invalid ticket id provided!");
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * This message is sent by the coordinator to shut down the server.
     */
    public static class MsgShutdown implements Command<Server> {
        @Override
        public void execute(Server obj) {
            // if the server have any Available (non reserved or sold ) ticket he should deallocate
            // them .
            // TODO :
            if (obj.NonReservedTickets > 0) {
                List<Ticket> ticketsToDeallocate = new ArrayList<>();
                for (Ticket ticket : obj.getAllocatedTickets()) {
                    if (ticket.getState().equals(State.AVAILABLE)) {
                        ticketsToDeallocate.add(ticket);
                    }
                }
                // return the available tickets to the DB
                obj.coordinator.getDatabase().deallocate(ticketsToDeallocate);
            }
            // put state of active to false so the termination steps are happining now
            obj.deactivateServer();
            // now the server has to complete the request of already reserved tickets ,this all will
            // happen in "execute of msgprocess".
            // return the tickets of aborted reservation immediatly to the data base etc.
            // we can do that because we can check the state of active before processing each request
            // so we know what to do.
        }
    }

    /**
     * This message is periodically sent by the {@link Estimator} to every server to inform
     * each server about the number of available tickets excluding those allocated to the
     * respective server itself.
     */
    public static class MsgTicketsAvailable implements Command<Server> {
        private final int numAvailable;

        /**
         * Constructs a new {@link MsgTicketsAvailable} message.
         * 
         * @param numAvailable The number of available tickets.
         */
        public MsgTicketsAvailable(final int numAvailable) {
            this.numAvailable = numAvailable;
        }

        @Override
        public void execute(Server obj) {
            /**
             * TODO: Update the number of available tickets and respond to the estimator
             * with the tickets currently available but allocated to this server.
             */
            // To inform the estimator, you must sent a `MsgAvailableServer` to its
            // mailbox. You can obtain this mailbox as follows:
            // final var mailbox = obj.coordinator.getEstimatorMailbox();

            // update the estimation of tickets
            obj.setCurrentTicketEstimation(numAvailable);
            int availableTicketAllocatedByServer = 0;
            // find out how many available ticket the server have
            for (Ticket ticket : obj.allocatedTickets) {
                if (ticket.getState().equals(State.AVAILABLE)) {
                    availableTicketAllocatedByServer++;
                }
            }
            // create he msg to send to estimator
            MsgAvailableServer msgAvailableServer = new MsgAvailableServer(obj.id, availableTicketAllocatedByServer);
            final var estimatormailbox = obj.coordinator.getEstimatorMailbox();
            // send the msg to mailbox of
            estimatormailbox.sendHighPriority(msgAvailableServer);

        }
    }

}
