package com.pseuco.np22.rocket;

import java.util.concurrent.locks.ReentrantLock;

import com.pseuco.np22.request.Request;
import com.pseuco.np22.request.RequestHandler;
import com.pseuco.np22.request.ServerId;
import com.pseuco.np22.request.Request.Kind;
import com.pseuco.np22.rocket.Server.MsgProcessRequest;

/**
 * <p>
 * Implementation of the load balancer.
 * </p>
 * 
 * <p>
 * The load balancer must implement {@link RequestHandler} for handling requests. Note
 * that the {@link handle} method may be called concurrently by our framework from within
 * different threads. Hence, your implementation should use proper synchronization to
 * avoid data races and other concurrency related problems.
 * </p>
 * 
 * <p>
 * The {@link Balancer} is constructed with a {@link Coordinator}, e.g., for querying
 * options, starting new servers, or retrieving the mailboxes of servers.
 * </p>
 */
public class Balancer implements RequestHandler {
    /**
     * The {@link Coordinator} of the ticket sales system.
     */
    private final Coordinator coordinator;

    private ReentrantLock switchGetKindLock = new ReentrantLock();
    private ReentrantLock switchGetMethodeLock = new ReentrantLock();
    private ReentrantLock caseGetServerLock = new ReentrantLock();
    private ReentrantLock defaultLock = new ReentrantLock();
    private ReentrantLock getLock = new ReentrantLock();
    private ReentrantLock postLock = new ReentrantLock();

    /**
     * Constructs a new {@link Balancer}.
     * 
     * @param coordinator The {@link Coordinator} of the ticket sales system.
     */
    public Balancer(final Coordinator coordinator) {
        this.coordinator = coordinator;
        // Scale to the number of initial servers.
        this.coordinator.scale(this.coordinator.getConfig().getInitialServers());
    }

    @Override
    public void handle(Request request) {
        switchGetKindLock.lock();
        try {
            /*
             * Implementation of the load balancer.
             * 
             * Hint: You must handle the `NUM_SERVERS` and `GET_SERVERS` requests here. All
             * other requests must be redirected to individual servers (expect the `DEBUG`
             * request which can be handled however you like).
             * 
             * For the `NUM_SERVERS` request you have to handle both `GET` and `POST` requests.
             */
            switch (request.getKind()) {

                case NUM_SERVERS: {
                    switchGetKindLock.unlock();

                    switchGetMethodeLock.lock();
                    switch (request.getMethod()) {

                        case GET: {
                            switchGetMethodeLock.unlock();
                            getLock.lock();
                            /*
                             * Query the coordinator for the number of active
                             * (non-terminating) servers and send the number back to
                             * the client using `respondWithInt`.
                             */
                            int numberofActiveServers = this.coordinator.getNumOfServers();
                            request.respondWithInt(numberofActiveServers);
                            getLock.unlock();
                            break;
                        }
                        case POST: {
                            switchGetMethodeLock.unlock();

                            postLock.lock();
                            /*
                             * Obtain the new number of servers from the request using
                             * `readInt`, scale to the given number of servers, and finally
                             * respond with the new number of servers.
                             */

                            final var numServers = request.readInt();
                            int IntnumberOFServer = numServers.get();

                            Boolean numberIsHere = false;
                            if (IntnumberOFServer > 0) {
                                numberIsHere = true;
                            }
                            if (numberIsHere) {
                                int newNumber = this.coordinator.scale(IntnumberOFServer);
                                request.respondWithInt(newNumber);
                                System.out.println("Done" + newNumber);
                                // In this case there is no need to scale of the number of servers
                            } else {
                                request.respondWithInt(this.coordinator.getNumOfServers());
                            }
                            postLock.unlock();
                            break;
                        }
                    }
                    break;
                }
                case GET_SERVERS: {
                    switchGetKindLock.unlock();
                    caseGetServerLock.lock();

                    /**
                     * Query the coordinator for the active (non-terminating) servers
                     * and send the ids of these servers to the client.
                     * 
                     * Hint: Use `respondWithServerIds`.
                     */
                    Iterable<ServerId> ids;
                    ids = this.coordinator.getActiveServerIds();
                    request.respondWithServerIds(ids);

                    caseGetServerLock.unlock();
                    break;
                }

                case DEBUG: {
                    /**
                     * You are free to handle this request however you like, e.g., by sending
                     * some useful debugging information to the client.
                     */
                    request.respondWithString("Happy Debugging! 🚫🐛");
                    break;
                }

                default:
                    switchGetKindLock.unlock();
                    defaultLock.lock();

                    /**
                     * The remaining requests must be handed over to a server.
                     * 
                     * Hint: Your implementation must be able to handle cases where a server
                     * already terminated or is in the process of terminating when a request
                     * for that server hits the load balancer. In case the server has already
                     * terminated, the balancer should assign a new server to the client.
                     * 
                     * You must use the coordinator to obtain the mailbox of the server which
                     * should handle the request. You must then use the mailbox of this server
                     * to deliver the request as a message with low priority. Using our
                     * skeleton this means constructing and sending a `MsgProcessRequest`
                     * message to the server.
                     */

                    // check if the request of client is worked from known Server
                    if (!request.getServerId().isEmpty()) {
                        // check if this server is now aktive or terminated
                        ServerId ID_associatedServerKnown = request.getServerId().get();

                        // TODO: Add by Mohamad, you to see editors in calsses Server and coordinator, I gave makr
                        // with "TODO"
                        boolean isServerStillActive = this.coordinator.getActiveServerIds()
                                .contains(ID_associatedServerKnown);
                        boolean isServerInProcesOfTermination = this.coordinator.getinTerminationServersIDs()
                                .contains(ID_associatedServerKnown);
                        if (isServerStillActive || isServerInProcesOfTermination) {
                            // constructing MsgProcessRequest with request
                            Command<Server> message = new MsgProcessRequest(request);
                            System.out.println("I BALANCER I send the Request " + request.getKind() + "  ");
                            var mailBoxOfassociatedServerKnown = this.coordinator
                                    .getServerMailbox(ID_associatedServerKnown);
                            mailBoxOfassociatedServerKnown.sendLowPriority(message);
                        } else {
                            // get random server from the list of active servers
                            ServerId associatedServerID = this.coordinator.pickRandomServer();
                            // correlate a customar with specific server
                            request.setServerId(associatedServerID);
                            // constructing MsgProcessRequest with request
                            Command<Server> message = new MsgProcessRequest(request);
                            System.out.println("I BALANCER I send the Request " + request.getKind() + "  ");
                            // get the mail box of this picked server
                            var mailBoxOfPickedServer = this.coordinator.getServerMailbox(associatedServerID);
                            // send this message with low priority
                            mailBoxOfPickedServer.sendLowPriority(message);
                        }
                        // if the rquest of client is new or the previous server is terminated, so then obtain a
                        // random active server to handle this request
                        // generally about isPresent etc.
                    } else {
                        // get random server from the list of active servers
                        ServerId associatedServerID = this.coordinator.pickRandomServer();
                        // correlate a customar with specific server
                        request.setServerId(associatedServerID);
                        // constructing MsgProcessRequest with request
                        Command<Server> message = new MsgProcessRequest(request);
                        System.out.println("I BALANCER I send the Request " + request.getKind() + "  ");
                        // get the mail box of this picked server
                        var mailBoxOfPickedServer = this.coordinator.getServerMailbox(associatedServerID);
                        // send this message with low priority
                        mailBoxOfPickedServer.sendLowPriority(message);
                    }
                    defaultLock.unlock();
                    break;

            }
        } finally {
            if (switchGetKindLock.isLocked()) {
                switchGetKindLock.unlock();
            }
            if (switchGetMethodeLock.isLocked()) {
                switchGetMethodeLock.unlock();
            }
            if (caseGetServerLock.isLocked()) {
                caseGetServerLock.unlock();
            }
            if (defaultLock.isLocked()) {
                defaultLock.unlock();
            }
            if (getLock.isLocked()) {
                getLock.unlock();
            }
            if (postLock.isLocked()) {
                postLock.unlock();
            }
        }

    }

}
