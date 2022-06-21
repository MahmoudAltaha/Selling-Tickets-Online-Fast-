package com.pseuco.np22.rocket;

/**
 * <p>
 * Generic interface for the
 * <a href="https://en.wikipedia.org/wiki/Command_pattern">command pattern</a>.
 * </p>
 * 
 * <p>
 * 📌 Hint: You do not have to use the command pattern for processing messages.
 * </p>
 */
public interface Command<O> {
    /**
     * Executes the command on the provided object.
     * 
     * @param obj The provided object.
     */
    void execute(O obj);
}
