package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.List;

import com.pseuco.np22.request.Request;
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
    private ServerState state;

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

    /**
     * List of the Reservations the server have to process.
     */
    private List<Reservation> currentReservations = new ArrayList<Reservation>();

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
        this.state = ServerState.ACTIVE;
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
        return this.NonReservedTickets;
    }

    /**
     * Return the status of the server (active or not)
     * 
     * @return The {@link active}
     */
    public boolean isActive() {
        return (this.state.equals(ServerState.ACTIVE));
    }

    /**
     * 
     * @return
     */
    public boolean isTerminated() {
        return this.state.equals(ServerState.TERMINATED);
    }

    /**
     * set the status of the Server to non active
     */
    private void deactivateServer() {
        this.state = ServerState.INTERMINATION;
    }

    /**
     * 
     */
    private void terminateServer() {
        this.state = ServerState.TERMINATED;
    }

    @Override
    public void run() {
        /*
         * TODO: Implement the server as described in the project description. The
         * server will process the messages sent to its mailbox.
         */

        // #TODO ((wrote by mahmoud))
        /*
         * i was trying to figure out how the server should possibly proccess MSGs,
         * i guess so ... then the logic in the execute methode of each msg. i still don't know
         * why i got that Interruptedexception
         * when i called the mailbox so i had to hadle it temporarly like that in a try catch
         * block.
         */
        try {
            boolean keepHandlingMsg = true;

            while (keepHandlingMsg) {
                Command<Server> message = getMailbox().recv();
                // make sure that the mailbox did not returns a null msg to avoid calling execute on null
                if (message != null) {
                    message.execute(this);
                }
                if (!isActive() && getMailbox().isEmpty()) {
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
            throw new RuntimeException("Not implemented!");
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
