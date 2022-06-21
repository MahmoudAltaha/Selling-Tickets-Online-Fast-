package com.pseuco.np22.rocket;

import com.pseuco.np22.Config;
import com.pseuco.np22.NoBonusException;
import com.pseuco.np22.request.RequestHandler;

/**
 * <p>
 * The entrypoint of your implementation.
 * </p>
 * 
 * <p>
 * ⚠️ This class must not be renamed and the signature of {@link launch} must not be
 * changed.
 * </p>
 */
public class Rocket {
    /**
     * Starts the ticket sales system.
     * 
     * 
     * @param config The configuration of the ticket sales system.
     * @param bonus  Indicates whether to use the bonus implementation.
     * @return The request handler (load balancer) of the system.
     */
    public static RequestHandler launch(Config config, boolean bonus) throws NoBonusException {
        if (bonus) {
            // Please keep this exception unless you implement something for the bonus
            // exercise. Our infrastructure tests whether it is thrown.
            throw new NoBonusException();
        }
        final var coordinator = new Coordinator(config);
        // Start the estimator in its own thread.
        (new Thread(coordinator.estimator)).start();
        // Return the load balancer for handling the requests.
        return coordinator.balancer;
    }
}
