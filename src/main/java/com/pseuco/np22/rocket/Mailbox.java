package com.pseuco.np22.rocket;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * A channel for messages of type {@code M} with two priorities.
 * </p>
 */
public class Mailbox<M> {

    private ArrayList<M> LowMailBox = new ArrayList<M>();
    private ArrayList<M> HighMailBox = new ArrayList<M>();
    private ReentrantLock MailboxLock = new ReentrantLock();
    private Condition IsMailboxFreeToAccess = MailboxLock.newCondition();

    /**
     * Constructs a new empty {@link Mailbox}.
     */
    public Mailbox() {

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
    public M recv() throws InterruptedException {
        // TODO ((mahmoud still not understand the dif. between
        // blocking and not))
        MailboxLock.lock();
        try {
            while ((LowMailBox.isEmpty() && HighMailBox.isEmpty())) {
                IsMailboxFreeToAccess.await();
            }
            M message = null;

            if (HighMailBox.size() > 0) {
                message = HighMailBox.get(0);

            } else if (LowMailBox.size() > 0) {
                message = LowMailBox.get(0);

            }

            return message;
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
    public M tryRecv() {
        MailboxLock.lock();

        try {
            M message = null;
            if (HighMailBox.size() > 0) {
                message = HighMailBox.get(0);
            } else if (LowMailBox.size() > 0) {
                message = LowMailBox.get(0);
            }
            return message;
        } finally {
            MailboxLock.unlock();
        }
    }
}
