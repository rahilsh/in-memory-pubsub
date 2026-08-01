package com.rahilsh.pubsub.api;

/**
 * A consumer of messages published to a {@code Topic}.
 *
 * <p>Implementations should be thread-safe if the same instance is registered
 * with more than one topic or subscribed multiple times.
 *
 * @param <T> the type of message this subscriber consumes
 */
public interface Subscriber<T> {

    /**
     * Handles a single delivered message.
     *
     * @param message the message payload; never {@code null}
     * @return {@code true} to acknowledge (ACK) successful processing,
     *         {@code false} to negatively acknowledge (NACK) and trigger a retry
     */
    boolean onMessage(T message);

    /**
     * Returns a stable identifier used for logging and diagnostics.
     *
     * @return a non-null, human-readable subscriber id
     */
    String getId();
}
