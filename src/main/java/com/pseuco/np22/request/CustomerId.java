package com.pseuco.np22.request;

import java.util.UUID;

import com.sun.net.httpserver.HttpExchange;

/**
 * <p>
 * A <em>customer id</em> uniquely identifies a customer.
 * </p>
 * 
 * <p>
 * 📌 Hint: The framework we provide takes care of generating and assigning customer ids.
 * </p>
 */
public class CustomerId {
    /**
     * The name of the HTTP header for the customer id.
     */
    protected static final String HEADER_NAME = "X-Customer-Id";

    /**
     * The underlying UUID of the customer.
     */
    private final UUID id;

    /**
     * Generates a new random customer id.
     * 
     * @return The generated customer id.
     */
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    /**
     * Extracts a customer id from an {@link HttpExchange} or assigns a random one.
     * 
     * @param exchange The {@link HttpExchange} to extract the id from.
     * @return The extracted or generated customer id.
     */
    protected static CustomerId fromHttpExchange(final HttpExchange exchange) {
        CustomerId customerId = null;
        for (var entry : exchange.getRequestHeaders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(CustomerId.HEADER_NAME)) {
                for (var value : entry.getValue()) {
                    try {
                        customerId = new CustomerId(UUID.fromString(value));
                    } catch (IllegalArgumentException error) {
                        // Client sent an invalid UUID – assign them a new one.
                        System.err.println("Warning: Received an invalid customer id!");
                    }
                    break;
                }
            }
            if (customerId != null) {
                break;
            }
        }
        if (customerId == null) {
            // The customer does not have an id yet, so we generate one.
            customerId = CustomerId.generate();
        }
        // Set the response header such that the client receives its customer id.
        exchange.getResponseHeaders().set(CustomerId.HEADER_NAME, customerId.id.toString());
        return customerId;
    }

    /**
     * Constructs a customer id from its underlying UUID.
     */
    public CustomerId(final UUID id) {
        this.id = id;
    }

    /**
     * Returns the UUID of the customer.
     * 
     * @return The UUID of the customer.
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
        return this.id.equals(((CustomerId) other).id);
    }

    @Override
    public String toString() {
        return String.format("CustomerId(%s)", this.id);
    }
}
