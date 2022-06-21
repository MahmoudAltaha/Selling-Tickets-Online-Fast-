package com.pseuco.np22.request;

/**
 * <p>
 * Interface for handling requests from a web browser.
 * </p>
 * 
 * <p>
 * The load balancer must implement this interface.
 * </p>
 */
public interface RequestHandler {
    /**
     * <p>
     * Handle a request from a web browser.
     * </p>
     * 
     * <p>
     * ⚠️ This method may be called from different threads!
     * </p>
     * 
     * @param request The {@link Request} to be handled.
     */
    public void handle(Request request);
}
