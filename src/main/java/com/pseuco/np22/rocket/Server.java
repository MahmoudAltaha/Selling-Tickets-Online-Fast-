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
     * Reservations made by customers.
     */
    private Map<CustomerId, Reservation> reservations = new HashMap<>();

    /**
     * List of allocated tickets from DB. //TODO think about allocateing tickets in case
     * Server/Or DB have no tickets left.
     */
    private List<Ticket> allocatedTickets = new ArrayList<Ticket>();

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
     * Methode returns the number of allocated tickets "non reserved or sold"
     */
    private int getNumAllocatedTickets() {
        return allocatedTickets.size();
    }

    /**
     * 
     * @return list with allocated available tickets
     */
    private List<Ticket> getAllocatedTickets() {
        return this.allocatedTickets;
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
     * 
     * @return
     */
    public boolean isInTermination() {
        serverStateLock.lock();
        try {
            return this.state.equals(ServerState.INTERMINATION);
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
                Command<Server> message = (Command<Server>) getMailbox().recv();
                assert (message != null);
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
                    int currentTicketEstimation = obj.currentTicketEstimation + obj.getNumAllocatedTickets();
                    request.respondWithInt(currentTicketEstimation);

                    break;
                }
                case RESERVE_TICKET: {

                    var customer = request.getCustomerId();
                    if (obj.reservations.containsKey(customer)) {
                        // We do not allow a customer to reserve more than a ticket at a time.
                        request.respondWithError("A ticket has already been reserved!");
                    } else if (obj.getNumAllocatedTickets() > 0 && obj.isActive()) {
                        // Take a ticket from the stack of available tickets and reserve it.
                        var ticket = obj.getAllocatedTickets().get(0);
                        obj.reservations.put(customer, new Reservation(ticket));
                        // Respond with the id of the reserved ticket.
                        request.respondWithInt(ticket.getId());

                    } else if (obj.getNumAllocatedTickets() == 0 && obj.isActive()) {
                        List<Ticket> tikets = obj.coordinator.getDatabase().allocate(10);
                        if (!tikets.isEmpty()) {
                            for (int i = 0; i < tikets.size(); i++) {
                                obj.getAllocatedTickets().add(tikets.get(0));
                            }
                            // Take a ticket from the stack of available tickets and reserve it.
                            var ticket = obj.getAllocatedTickets().get(0);
                            obj.reservations.put(customer, new Reservation(ticket));
                            // Respond with the id of the reserved ticket.
                            request.respondWithInt(ticket.getId());

                        } else {
                            // Tell the client that no tickets are available.
                            request.respondWithSoldOut();
                        }

                    } else if (obj.isInTermination()) {
                        ServerId newServerIdToHandleThisRequest = obj.coordinator.pickRandomServer();
                        request.setServerId(newServerIdToHandleThisRequest);
                        request.respondWithError("this server is down");
                    }
                    break;

                } // TODO :we have to get the abort ticket back to data base direct, only if ther server
                  // nonactive state
                case ABORT_PURCHASE: {
                    var customer = request.getCustomerId();
                    if (!obj.reservations.containsKey(customer)) {
                        // Without a reservation there is nothing to abort.
                        request.respondWithError("No ticket has been reserved!");
                    } else {
                        var reservation = obj.reservations.get(customer);
                        var ticketId = request.readInt();
                        if (ticketId.isEmpty()) {
                            // The client is supposed to provide a ticket id.
                            request.respondWithError("No ticket id provided!");
                        } else if (ticketId.get() == reservation.getTicketId()) {
                            // Abort the reservation and put the ticket back on the allocatedTickets.
                            var ticket = reservation.abort();
                            if (obj.isInTermination()) {
                                List<Ticket> Tickettolist = new ArrayList<Ticket>();
                                Tickettolist.add(ticket);
                                obj.coordinator.getDatabase().deallocate(Tickettolist);

                            } else {
                                obj.allocatedTickets.add(ticket);
                            }
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
                    var customer = request.getCustomerId();
                    if (!obj.reservations.containsKey(customer)) {
                        // Without a reservation there is nothing to buy.
                        request.respondWithError("No ticket has been reserved!");
                    } else {
                        var reservation = obj.reservations.get(customer);
                        var ticketId = request.readInt();
                        if (ticketId.isEmpty()) {
                            // The client is supposed to provide a ticket id.
                            request.respondWithError("No ticket id provided!");
                        } else if (ticketId.get() == reservation.getTicketId()) {
                            // Sell the ticket to the customer.
                            var ticket = reservation.sell();
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
                default:
                    break;
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

            if (!obj.getAllocatedTickets().isEmpty()) {
                obj.coordinator.getDatabase().deallocate(obj.getAllocatedTickets());
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

            // respond to estimator in these steps :

            // 1) find out how many available ticket the server have

            int availableTicketAllocatedByServer = obj.getAllocatedTickets().size();

            // 2) create he msg to send to estimator
            Command<Estimator> msgAvailableServer = new MsgAvailableServer(obj.id, availableTicketAllocatedByServer);
            var estimatormailbox = obj.coordinator.getEstimatorMailbox();
            // 3) send the msg to mailbox of estimator
            estimatormailbox.sendHighPriority(msgAvailableServer);
        }
    }

}
