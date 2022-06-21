package com.pseuco.np22.request;

import java.io.IOException;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

/**
 * <p>
 * Represents a request from a web browser.
 * </p>
 * 
 * <p>
 * 📌 Hint: Your implementation primarily interacts with instances of this class.
 * </p>
 * 
 * <p>
 * Every request has a {@link Method} and a {@link Kind}. The {@link Method} indicates
 * whether a request is merely for retrieving information or may have side effects like
 * scaling the number of servers or reserving a ticket.
 * </p>
 */
public class Request {
    /**
     * The <em>method</em> of the request.
     */
    public enum Method {
        /**
         * <p>
         * The GET method is used to retrieve some information from the system.
         * </p>
         * 
         * <p>
         * A GET request must not have any side effects.
         * </p>
         */
        GET,
        /**
         * <p>
         * The POST method is used to send some information to the system.
         * </p>
         * 
         * <p>
         * A POST request may have side effects (e.g., reserving a ticket).
         * </p>
         */
        POST;

        /**
         * Returns a {@link Method} based on its name.
         * 
         * @param method The name of the method.
         * 
         * @return An optional {@link Method} which is empty in case the string is invalid.
         */
        public static Optional<Method> fromName(final String method) {
            switch (method) {
                case "GET":
                    return Optional.of(Method.GET);
                case "POST":
                    return Optional.of(Method.POST);
                default:
                    return Optional.empty();
            }
        }
    }

    /**
     * There are seven kinds of requests.
     */
    public enum Kind {
        /**
         * <p>
         * Retrieves or sets the number of servers for on-demand scaling.
         * </p>
         * 
         * <p>
         * 📌 Hint: Should be processed by the load balancer.
         * </p>
         */
        NUM_SERVERS,
        /**
         * <p>
         * Retrieves a list of all servers which are active, i.e., not terminating.
         * </p>
         * 
         * <p>
         * 📌 Hint: Should be processed by the load balancer.
         * </p>
         */
        GET_SERVERS,

        /**
         * <p>
         * Retrieves an approximation of the number of available tickets.
         * </p>
         * 
         * <p>
         * 📌 Hint: Should be processed by a server.
         * </p>
         */
        NUM_AVAILABLE_TICKETS,

        /**
         * <p>
         * Reserves a ticket.
         * </p>
         * 
         * <p>
         * 📌 Hint: Should be processed by a server.
         * </p>
         */
        RESERVE_TICKET,

        /**
         * <p>
         * Buys a previously reserved ticket.
         * </p>
         * 
         * <p>
         * 📌 Hint: Should be processed by a server.
         * </p>
         */
        BUY_TICKET,

        /**
         * <p>
         * Aborts the purchase of a previously reserved ticket.
         * </p>
         * 
         * <p>
         * 📌 Hint: Should be processed by a server.
         * </p>
         */
        ABORT_PURCHASE,

        /**
         * <p>
         * Useful for sending information for debugging.
         * </p>
         * 
         * <p>
         * 📌 Hint: You can process this request however you like.
         * </p>
         */
        DEBUG;

        /**
         * Returns a {@link Kind} based on its path.
         * 
         * @param path The path.
         * 
         * @return An optional {@link Kind} which is empty in case the path is invalid.
         */
        public static Optional<Kind> fromPath(final String path) {
            switch (path) {
                case "/api/admin/num_servers":
                    return Optional.of(Kind.NUM_SERVERS);
                case "/api/admin/get_servers":
                    return Optional.of(Kind.GET_SERVERS);

                case "/api/num_available_tickets":
                    return Optional.of(Kind.NUM_AVAILABLE_TICKETS);

                case "/api/reserve_ticket":
                    return Optional.of(Kind.RESERVE_TICKET);
                case "/api/buy_ticket":
                    return Optional.of(Kind.BUY_TICKET);
                case "/api/abort_purchase":
                    return Optional.of(Kind.ABORT_PURCHASE);
            }
            if (path.startsWith("/api/debug")) {
                return Optional.of(Kind.DEBUG);
            }
            return Optional.empty();
        }
    }

    /**
     * The method of the request.
     */
    private final Method method;
    /**
     * The kind of the request.
     */
    private final Kind kind;

    /**
     * The underlying {@link HttpExchange} used for communication.
     */
    private final HttpExchange exchange;

    /**
     * The customer id associated with the request.
     */
    private final CustomerId customerId;

    /**
     * An optional server id associated with the request.
     */
    private Optional<ServerId> serverId;

    /**
     * Tracks whether a response has already been sent.
     */
    private boolean responseSent = false;

    /**
     * Constructs a new request from the provided parameters.
     * 
     * @param method   The method of the request.
     * @param kind     The kind of the request.
     * @param exchange The {@link HttpExchange} used for communication.
     */
    public Request(final Method method, final Kind kind, final HttpExchange exchange) {
        this.method = method;
        this.kind = kind;
        this.exchange = exchange;
        // Extract or generate a customer id.
        this.customerId = CustomerId.fromHttpExchange(this.exchange);
        // Extract any provided server id.
        this.serverId = ServerId.fromHttpExchange(this.exchange);
    }

    /**
     * Returns the {@link Method} of the request.
     * 
     * @return The {@link Method} of the request.
     */
    public Method getMethod() {
        return this.method;
    }

    /**
     * Returns the {@link Kind} of the request.
     * 
     * @return The {@link Kind} of the request.
     */
    public Kind getKind() {
        return this.kind;
    }

    /**
     * Returns the path of the request.
     * 
     * @return The path of the request.
     */
    public String getPath() {
        return this.exchange.getRequestURI().getPath();
    }

    /**
     * Returns the {@link CustomerId} associated with the request.
     * 
     * @return The {@link CustomerId} associated with the request.
     */
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    /**
     * Returns the {@link ServerId} associated with the request if there is any.
     * 
     * @return The {@link ServerId} associated with the request if there is any.
     */
    public Optional<ServerId> getServerId() {
        return this.serverId;
    }

    /**
     * <p>
     * Sets the server id.
     * </p>
     * 
     * <p>
     * This method should be called before responding to the request.
     * </p>
     * 
     * <p>
     * 📌 Hint: Call this method to assign a server to a client.
     * </p>
     * 
     * @param serverId The {@link ServerId} to set.
     */
    public void setServerId(final ServerId serverId) {
        this.serverId = Optional.of(serverId);
        this.exchange.getResponseHeaders().set(ServerId.HEADER_NAME, serverId.getUUID().toString());
    }

    /**
     * <p>
     * Reads an integer provided by the web browser (e.g., a ticket id or number of servers).
     * </p>
     * 
     * <p>
     * In case the browser did not provide an integer, an empty {@link Optional} is returned.
     * </p>
     * 
     * <p>
     * 📌 Hint: This method has side effects and should be called only once on each request.
     * </p>
     * 
     * @return The integer if there is any.
     */
    public Optional<Integer> readInt() {
        try {
            final var body = new String(this.exchange.getRequestBody().readAllBytes());
            return Optional.of(Integer.parseInt(body));
        } catch (IOException error) {
            return Optional.empty();
        } catch (NumberFormatException error) {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * Sends a response to the client.
     * </p>
     * 
     * <p>
     * This method blocks until the response has been sent.
     * </p>
     * 
     * <p>
     * 📌 Hint: You do not need to call this method directly. Instead, use any of the other
     * methods for sending a response with specific data.
     * </p>
     * 
     * @param code The HTTP status code of the response.
     * @param data The data to send to the client.
     */
    public void respond(final int code, final String data) {
        assert !this.responseSent : "A response has already been sent.";
        this.responseSent = true;

        final var bytes = data.getBytes();
        try {
            this.exchange.sendResponseHeaders(code, bytes.length);
            final var body = this.exchange.getResponseBody();
            body.write(bytes);
            body.close();
        } catch (IOException error) {
            System.err.println("Warning: Error responding to client.");
        }
    }

    /**
     * <p>
     * Responds with an error indicating an invalid request to the client.
     * </p>
     * 
     * <p>
     * This method blocks until the response has been sent.
     * </p>
     * 
     * @param message An optional error message to be sent to the client.
     */
    public void respondWithError(final String message) {
        this.respond(400, message == null ? "" : message);
    }

    /**
     * <p>
     * Responds with an integer, e.g., a ticket number or the number of servers.
     * </p>
     * 
     * <p>
     * This method blocks until the response has been sent.
     * </p>
     * 
     * @param integer The integer to be sent to the client.
     */
    public void respondWithInt(final int integer) {
        this.respond(200, Integer.toString(integer));
    }

    /**
     * <p>
     * Responds with an arbitrary string.
     * </p>
     * 
     * <p>
     * This method blocks until the response has been sent.
     * </p>
     * 
     * @param string The string with which to respond.
     */
    public void respondWithString(final String string) {
        this.respond(200, string);
    }

    /**
     * <p>
     * Responds with the message `SOLD OUT`.
     * </p>
     * 
     * <p>
     * Use this method to respond to a reservation request when no tickets available.
     * </p>
     * 
     * <p>
     * This method blocks until the response has been sent.
     * </p>
     */
    public void respondWithSoldOut() {
        this.respond(200, "SOLD OUT");
    }

    /**
     * <p>
     * Responds with a list of server ids.
     * </p>
     * 
     * <p>
     * Use this method to send a list of server ids to the client.
     * </p>
     * 
     * @param ids An {@link Iterable} of server ids.
     */
    public void respondWithServerIds(final Iterable<ServerId> ids) {
        final var serverList = new StringBuilder();
        for (var serverId : ids) {
            serverList.append(serverId.getUUID().toString());
            serverList.append('\n');
        }
        this.respond(200, serverList.toString());
    }
}
