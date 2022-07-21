package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pseuco.np22.request.ServerId;
import com.pseuco.np22.rocket.Server.MsgTicketsAvailable;

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
     * map which contains the serverID and the estimation we got from that server.
     */
    private HashMap<ServerId, Integer> serverEstimations = new HashMap<>();

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
         * Implement the estimator as described in the project description. The
         * estimator will periodically send messages to the servers and process the
         * messages from its own mailbox.
         */
        while (true) {
            // List of non terminated servers,,,,after each iterate we reset it
            HashMap<ServerId, Server> nonTerminatedServers = new HashMap<>();
            List<ServerId> nonTerminatedServersIds = new ArrayList<>();
            // reset the old estimation.
            this.serverEstimations.clear();

            // check for non terminated servers
            for (ServerId serverId : this.coordinator.getAllServerIds()) {
                Server serverToCheck = this.coordinator.getAllServers().get(serverId);
                if (!serverToCheck.isTerminated()) {
                    nonTerminatedServers.put(serverId, serverToCheck);
                    nonTerminatedServersIds.add(serverId);
                }
            }
            // now get the num of tickets in DB
            int numberofTicketsInDB = this.coordinator.getDatabase().getNumAvailable();
            // read the msgs to estimate from each server that is still not terminated....in first
            // round it is empty.
            while (!this.getMailbox().isEmpty()) {
                MsgAvailableServer msg = (MsgAvailableServer) this.getMailbox().tryRecv();
                assert (msg != null);
                msg.execute(this);
            }
            // send all servers the estimation number (except the number of ticket the server we send
            // to has itself)

            for (ServerId serverId : nonTerminatedServersIds) {
                int numberOfTicketInServers = 0;
                if (!serverEstimations.isEmpty()) {
                    for (ServerId id : serverEstimations.keySet()) {
                        if (!id.equals(serverId)) {
                            numberOfTicketInServers = numberOfTicketInServers + serverEstimations.get(id);
                        }
                    }
                }
                // create the msg to send
                int endEstimation = numberOfTicketInServers + numberofTicketsInDB;
                Command<Server> msgTicketsAvailable = new MsgTicketsAvailable(endEstimation);
                nonTerminatedServers.get(serverId).getMailbox().sendHighPriority(msgTicketsAvailable);
            }

            // wait 10/ nonTerminatedServers.Size */
            double secondsToSleep = 10 / nonTerminatedServers.size();
            double millisecondsToSleep = secondsToSleep * 1000;
            try {
                Thread.sleep((long) millisecondsToSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
            obj.serverEstimations.put(serverId, numAvailable);
        }
    }
}
