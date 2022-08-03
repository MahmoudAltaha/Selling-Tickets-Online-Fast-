package com.pseuco.np22.rocket;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * A channel for messages of type {@code M} with two priorities.
 * </p>
 */
public class Mailbox<M> {

    private Queue<M> LowMailBox = new LinkedList<>();
    private Queue<M> HighMailBox = new LinkedList<>();;
    private ReentrantLock MailboxLock;
    private Condition IsThereMessageToRecev;

    /**
     * if The server is active or inTermination then it's mailbox should be open otherwise the
     * mail box is closed
     */
    public static enum MailboxState {
        OPEN,
        CLOSED,
    }

    private MailboxState mailboxState = MailboxState.OPEN;

    /**
     * mailbox state locks
     */
    private ReentrantLock mailboxStateLock = new ReentrantLock();

    /**
     * Check if the Mail Box OPEN
     */
    private boolean isMailboxOpen() {
        mailboxStateLock.lock();
        try {
            if (mailboxState.equals(MailboxState.OPEN)) {
                return true;
            } else {
                return false;
            }
        } finally {
            mailboxStateLock.unlock();
        }
    }

    /**
     * Change the state of MailBox to CLOSED
     */
    public void closingMailBox() {
        mailboxStateLock.lock();
        try {
            mailboxState = MailboxState.CLOSED;
        } finally {
            mailboxStateLock.unlock();
        }
    }

    /**
     * Constructs a new empty {@link Mailbox}.
     */
    public Mailbox() {
        this.MailboxLock = new ReentrantLock();
        this.IsThereMessageToRecev = MailboxLock.newCondition();
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
            if (isMailboxOpen()) {
                boolean messageAdd = LowMailBox.add(message);
                IsThereMessageToRecev.signal();
                return messageAdd;
            } else {
                return false;
            }

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
            if (isMailboxOpen()) {
                boolean messageAdd = HighMailBox.add(message);
                IsThereMessageToRecev.signal();
                return messageAdd;
            } else {
                return false;
            }
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
        MailboxLock.lock();
        try {
            while ((LowMailBox.isEmpty() && HighMailBox.isEmpty())) {
                IsThereMessageToRecev.await();
            }
            M message = null;

            if (HighMailBox.size() > 0) {
                message = HighMailBox.poll();

            } else if (LowMailBox.size() > 0) {
                message = LowMailBox.poll();

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
                message = HighMailBox.poll();
            } else if (LowMailBox.size() > 0) {
                message = LowMailBox.poll();
            }
            return message;
        } finally {
            MailboxLock.unlock();
        }
    }
}
