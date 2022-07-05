package com.pseuco.np22.rocket;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * A channel for messages of type {@code M} with two priorities.
 * </p>
 */
public class Mailbox<M> {

    PriorityQueue<M> LowMailBox;
    PriorityQueue<M> HighMailBox;
    private Lock MailboxLock;
    private Condition IsMailboxFreeToAccess;

    /**
     * Constructs a new empty {@link Mailbox}.
     */
    public Mailbox() {
        this.LowMailBox = new PriorityQueue<>();
        this.HighMailBox = new PriorityQueue<>();
        this.MailboxLock = new ReentrantLock();
        this.IsMailboxFreeToAccess = MailboxLock.newCondition();
    }

    /**
     * Returns whether the mailbox is empty.
     * 
     * @return Whether the mailbox is empty.
     */
    public boolean isEmpty() {
        return (LowMailBox.isEmpty() && HighMailBox.isEmpty());
    }

    /**
     * Tries to send a message with low priority.
     * 
     * @param message The message.
     * @return Indicates whether the message has been sent.
     */
    public boolean sendLowPriority(M message) {
        return LowMailBox.add(message);
    }

    /**
     * Ties to send a message with high priority.
     * 
     * @param message The message.
     * @return Indicates whether the message has been sent.
     */
    public boolean sendHighPriority(M message) {
        return HighMailBox.add(message);
    }

    /**
     * <p>
     * Receives a message blocking the receiving thread.
     * </p>
     * 
     * <p>
     * 📌 Hint: This is useful for the {@link Server}
     * </p>
     * 
     * @return The received message.
     * @throws InterruptedException The thread has been interrupted.
     */
    public M recv() throws InterruptedException {
        M message = null;
        try {
            if (HighMailBox.size() > 0) {
                message = HighMailBox.poll();
            } else if (LowMailBox.size() > 0) {
                message = LowMailBox.poll();
            }
        } catch (Exception InterruptedException) {
            return null;
        }
        return message;
    }

    /**
     * <p>
     * Tries to receive a message without blocking.
     * </p>
     * 
     * <p>
     * 📌 Hint: This is useful for the {@link Estimator}.
     * </p>
     * 
     * @return The received message or {@code null} in case the {@link Mailbox} is empty.
     */
    public M tryRecv() {
        M message = null;
        try {
            if (HighMailBox.size() > 0) {
                message = HighMailBox.poll();
            } else if (LowMailBox.size() > 0) {
                message = LowMailBox.poll();
            }
        } catch (Exception InterruptedException) {
            return null;
        }
        return message;
    }

    /**
     * Lock MailBox of Low priority
     */
    public void MailboxLock() {
        MailboxLock.lock();
        ;
    }

    /**
     * Unlock MailBox of Low priority
     */
    public void MailboxUnLock() {
        MailboxLock.unlock();
        ;
    }

    /**
     * get Condition MailBox of Low priority
     */
    public Condition IsMailboxFreeToAccess() {
        return IsMailboxFreeToAccess;
    }

}
