package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    private int currentTicketsInSystem = 0;

    /**
     * map which contains the serverID and the estimation we got from that server.
     */
    private final HashMap<ServerId, Integer> serverEstimations = new HashMap<>();

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

    /**
     * add the num of tickets of server/DB to the current estimation
     * 
     * @param i number of ticket we need to add to the current estimation
     */
    private void addToCurrentTicketsEstimation(int i) {
        this.currentTicketsInSystem = this.currentTicketsInSystem + i;
    }

    /**
     * use this methode to reset the estimation befor a new round of collecting info about
     * tickets
     */
    private void resetCurrentTicketInSystem() {
        this.currentTicketsInSystem = 0;
    }

    @Override
    public void run() {
        /*
         * TODO: Implement the estimator as described in the project description. The
         * estimator will periodically send messages to the servers and process the
         * messages from its own mailbox.
         */
        while (true) {
            // List of non terminated servers,,,,after each iterate we reset it
            HashMap<ServerId, Server> nonTerminatedServers = new HashMap<>();
            List<ServerId> nonTerminatedServersIds = new ArrayList<>();
            // reset the old estimation.
            this.resetCurrentTicketInSystem();
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
            this.addToCurrentTicketsEstimation(numberofTicketsInDB);
            // read the msgs to estimate from each server that is still not terminated....in first
            // round it is empty.
            while (!this.getMailbox().isEmpty()) {
                Command<Estimator> msg = this.getMailbox().tryRecv();
                msg.execute(this);
            }
            // send all servers the estimation number (except the number of ticket the server we send
            // to has itself)
            int numberOfTicketInServers = 0;

            for (ServerId serverId : nonTerminatedServersIds) {
                for (ServerId id : serverEstimations.keySet()) {
                    if (!id.equals(serverId)) {
                        numberOfTicketInServers = numberOfTicketInServers + serverEstimations.get(id);
                    }
                }
                // create the msg to send
                int endEstimation = numberOfTicketInServers + numberofTicketsInDB;
                MsgTicketsAvailable msgTicketsAvailable = new MsgTicketsAvailable(endEstimation);
                // TODO: Mohamaaaad code
                var mailboxofserver = this.coordinator.getServerMailbox(serverId);
                mailboxofserver.sendHighPriority(msgTicketsAvailable);
                // TODO: mohamuuud code
                // nonTerminatedServers.get(serverId)..getMailbox().sendHighPriority(msgTicketsAvailable);
            }

            // wait 10/ nonTerminatedServers.Size */
            double secondsToSleep = 10 / nonTerminatedServers.size();
            double millisecondsToSleep = secondsToSleep * 1000;
            try {
                Thread.sleep((long) millisecondsToSleep);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
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
