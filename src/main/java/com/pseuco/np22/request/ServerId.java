package com.pseuco.np22.request;

import java.util.Optional;
import java.util.UUID;

import com.sun.net.httpserver.HttpExchange;

/**
 * <p>
 * A <em>served id</em> uniquely identifies a server.
 * </p>
 * 
 * <p>
 * 📌 Hint: You need to generate an id whenever the load balancer starts a new server.
 * </p>
 */
public class ServerId {
    /**
     * The name of the HTTP header for the server id.
     */
    protected static final String HEADER_NAME = "X-Server-Id";

    /**
     * The underlying UUID of the server id.
     */
    private final UUID id;

    /**
     * Generates a new random server id.
     * 
     * @return The generated server id.
     */
    public static ServerId generate() {
        return new ServerId(UUID.randomUUID());
    }

    /**
     * <p>
     * Extracts a server id from an {@link HttpExchange}.
     * </p>
     * 
     * <p>
     * If the client has not been assigned a server yet, the id may be empty.
     * </p>
     * 
     * @param exchange The {@link HttpExchange} to extract the id from.
     * @return The extracted server id.
     */
    protected static Optional<ServerId> fromHttpExchange(final HttpExchange exchange) {
        ServerId serverId = null;
        for (var entry : exchange.getRequestHeaders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(ServerId.HEADER_NAME)) {
                for (var value : entry.getValue()) {
                    try {
                        serverId = new ServerId(UUID.fromString(value));
                    } catch (IllegalArgumentException error) {
                        // Client sent an invalid UUID – assign them a new one.
                        System.err.println("Warning: Received an invalid server id!");
                    }
                    break;
                }
            }
            if (serverId != null) {
                break;
            }
        }
        if (serverId != null) {
            exchange.getResponseHeaders().set(ServerId.HEADER_NAME, serverId.id.toString());
            return Optional.of(serverId);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Constructs a server id from its underlying UUID.
     */
    public ServerId(final UUID id) {
        this.id = id;
    }

    /**
     * Returns the UUID of the server.
     * 
     * @return The UUID of the server.
     */
    public UUID getUUID() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        return this.id.equals(((ServerId) other).id);
    }

    @Override
    public String toString() {
        return String.format("ServerId(%s)", this.id);
    }
}
