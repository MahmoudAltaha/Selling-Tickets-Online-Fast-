package com.pseuco.np22.rocket;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * A channel for messages of type {@code M} with two priorities.
 * </p>
 */
public class Mailbox<M> {

    private PriorityQueue<M> LowMailBox;
    private PriorityQueue<M> HighMailBox;
    private ReentrantLock MailboxLock;
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
        MailboxLock.lock();
        try {
            return (LowMailBox.isEmpty() && HighMailBox.isEmpty());
        } finally {
            MailboxLock.unlock();
        }

    }

    /**
     * Tries to send a message with low priority.
     * 
     * @param message The message.
     * @return Indicates whether the message has been sent.
     */
    public boolean sendLowPriority(M message) {
        MailboxLock.lock();
        try {
            boolean messageAdd = LowMailBox.add(message);
            IsMailboxFreeToAccess.signal();
            return messageAdd;

        } finally {
            MailboxLock.unlock();
        }

    }

    /**
     * Ties to send a message with high priority.
     * 
     * @param message The message.
     * @return Indicates whether the message has been sent.
     */
    public boolean sendHighPriority(M message) {
        MailboxLock.lock();
        try {
            boolean messageAdd = HighMailBox.add(message);
            IsMailboxFreeToAccess.signal();
            return messageAdd;

        } finally {
            MailboxLock.unlock();
        }

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
    public Command<Server> recv() throws InterruptedException { // TODO ((mahmoud still not understand the dif. between
                                                                // blocking and
        // not))
        MailboxLock.lock();
        try {
            while ((LowMailBox.isEmpty() && HighMailBox.isEmpty())) {
                IsMailboxFreeToAccess.await();
            }
            Command<Server> message = null;

            if (HighMailBox.size() > 0) {
                message = (Command<Server>) HighMailBox.poll();
                return message;
            } else if (LowMailBox.size() > 0) {
                message = (Command<Server>) LowMailBox.poll();
                return message;
            }

            return null;
        } finally {
            MailboxLock.unlock();
        }

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
    public Command<Estimator> tryRecv() {
        MailboxLock.lock();

        try {
            Command<Estimator> message = null;
            if (HighMailBox.size() > 0) {
                message = (Command<Estimator>) HighMailBox.poll();
                return message;
            } else if (LowMailBox.size() > 0) {
                message = (Command<Estimator>) LowMailBox.poll();
                return message;
            }
            return null;
        } finally {
            MailboxLock.unlock();
        }
    }
}
