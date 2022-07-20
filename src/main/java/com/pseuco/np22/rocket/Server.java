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

    /**
     * Server state locks
     */
    private ReentrantLock serverStateLock = new ReentrantLock();

    /**
     * Reservations made by customers.
     */
    private Map<CustomerId, Reservation> reservations = new HashMap<>();

    /**
     * List of allocated tickets from DB.
     * Server/Or DB have no tickets left.
     */
    private List<Ticket> allocatedTickets = new ArrayList<>();

    /*
     * Define Server states
     */
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

    /*
     * Start the Serve with active state
     */
    private ServerState state = ServerState.ACTIVE;

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
     * Return true if the status of the server "Activ"
     * 
     * @return The {@link Active}
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
     * Return true if the status of the server "Terminated"
     * 
     * @return The {@link Terminated}
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
     * Return true if the status of the server "in Proces of Termaination"
     * 
     * @return The {@link isInTermination}
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
     * set the status of the Server to In proces of Termaination
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
     * set the status of the Server to Terminated
     */
    // TODO: add by Mohamad the line "removefromInTermination"
    // I send all request from Balanacer to the server, except that is terminated
    // so bevor get this thread interupted, I remove this server from "Intermination List"
    // in the coordinator. In this case the balancer send the request to other active server.
    //
    private void terminateServer() {
        serverStateLock.lock();
        try {
            this.state = ServerState.TERMINATED;
            this.coordinator.addToTerminatedServerIds(this.id);
            this.coordinator.removefromInTermination(this.id);
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
         * The server will process the messages sent to its mailbox.
         */
        try {
            boolean keepHandlingMsg = true;
            // Get initial number of tickets from the data base
            List<Ticket> tikets = new ArrayList<>();
            tikets = this.coordinator.getDatabase().allocate(10);
            if (!tikets.isEmpty()) {
                int stodForLoop = tikets.size();
                for (int i = 0; i < stodForLoop; i++) {
                    this.allocatedTickets.add(tikets.remove(0));
                }
            }
            System.out.println("Ticket from DB when ther Server Start : " + this.allocatedTickets.size());
            // Start handling the request
            while (keepHandlingMsg) {
                Command<Server> message = (Command<Server>) getMailbox().recv();
                assert (message != null);
                // make sure that the mailbox did not returns a null msg to avoid calling execute on null
                if (message != null) {
                    message.execute(this);
                }
                if (!isActive() && this.reservations.isEmpty() && this.getMailbox().isEmpty()) {
                    keepHandlingMsg = false;
                }
            }
            // the server was in Terminating state and he has finished handling existing requests so
            // he could now terminate
            this.terminateServer();
            Thread.interrupted();

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            Thread.interrupted();
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

                    final var customer = request.getCustomerId();
                    if (obj.reservations.containsKey(customer)) {
                        // We do not allow a customer to reserve more than a ticket at a time.

                        request.respondWithError("A ticket has already been reserved!");

                    } else if (obj.getNumAllocatedTickets() > 0 && obj.isActive()) {

                        // Take a ticket from the stack of available tickets and reserve it.
                        final var ticket = obj.getAllocatedTickets().remove(0);
                        obj.reservations.put(customer, new Reservation(ticket));

                        // Respond with the id of the reserved ticket.
                        request.respondWithInt(ticket.getId());
                        // there is no tickets localy but I am Activ, so I have to get tickets from DB
                    } else if (obj.getNumAllocatedTickets() == 0 && obj.isActive()) {
                        List<Ticket> tikets = new ArrayList<>();
                        tikets = obj.coordinator.getDatabase().allocate(10);
                        // Check if I get Tickets from DB or not
                        if (!tikets.isEmpty()) {
                            // Yes I get, so save it localy
                            int stodForLoop = tikets.size();
                            for (int i = 0; i < stodForLoop; i++) {
                                obj.allocatedTickets.add(tikets.remove(0));
                            }
                            // No I did not get tickets
                        } else {
                            // Tell the client that no tickets are available.
                            request.respondWithSoldOut();
                        }
                        // In this case I am checking if I am in proces of termination
                    } else if (obj.isInTermination()) {
                        // Yes I am in proces of termination, so I have send the requesst to other active server
                        ServerId newServerIdToHandleThisRequest = obj.coordinator.pickRandomServer();
                        request.setServerId(newServerIdToHandleThisRequest);
                        request.respondWithError("this server is down");
                        System.out.println("this server is down");
                    }
                    break;

                }
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
                            // I did abort, but I have to check if I return the abort ticket to DB or save it localy
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
             * Update the number of available tickets and respond to the estimator
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
            MsgAvailableServer msgAvailableServer = new MsgAvailableServer(obj.id, availableTicketAllocatedByServer);
            var estimatormailbox = obj.coordinator.getEstimatorMailbox();

            // 3) send the msg to mailbox of estimator
            estimatormailbox.sendHighPriority(msgAvailableServer);
        }
    }

}
