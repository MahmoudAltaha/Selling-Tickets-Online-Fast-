package com.pseuco.np22.rocket;

import com.pseuco.np22.request.ServerId;

/**
 * <p>
 * The {@link Estimator} estimates the number of tickets available overall.
 * </p>
 */
public class Estimator implements Runnable {
    /**
     * The {@link Coordinator} of the ticket sales system.
     */
    private final Coordinator coordinator;

    /**
     * The mailbox of the {@link Estimator}.
     */
    private final Mailbox<Command<Estimator>> mailbox = new Mailbox<>();

    /**
     * Constructs a new {@link Estimator}.
     * 
     * @param coordinator The {@link Coordinator} of the ticket sales system.
     */
    public Estimator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Returns the {@link Mailbox} of the estimator.
     * 
     * @return The {@link Mailbox} of the estimator.
     */
    public Mailbox<Command<Estimator>> getMailbox() {
        return this.mailbox;
    }

    @Override
    public void run() {
        /*
         * TODO: Implement the estimator as described in the project description. The
         * estimator will periodically send messages to the servers and process the
         * messages from its own mailbox.
         */
        throw new RuntimeException("Not implemented!");
    }

    /**
     * A message informing the {@link Estimator} about tho number of available tickets
     * allocated to a particular server.
     */
    public static class MsgAvailableServer implements Command<Estimator> {
        private final ServerId serverId;
        private final int numAvailable;

        /**
         * Constructs a new {@link MsgAvailableServer} message.
         * 
         * @param serverId     The id of the server.
         * @param numAvailable The number of tickets available on the server.
         */
        public MsgAvailableServer(final ServerId serverId, final int numAvailable) {
            this.serverId = serverId;
            this.numAvailable = numAvailable;
        }

        @Override
        public void execute(Estimator obj) {
            throw new RuntimeException("Not implemented!");
        }
    }
}
