package com.rahilsh.pubsub.core;

import com.rahilsh.pubsub.api.Subscriber;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A named, bounded message channel that fans out published messages to all of
 * its registered {@link Subscriber subscribers}.
 *
 * <p>A topic owns its buffer but not its thread. Both the buffer (via a
 * {@link Supplier}) and the dedicated single-thread {@link ExecutorService} that
 * drains it are injected, keeping the topic decoupled from concrete concurrency
 * primitives (IoC).
 *
 * <p>Delivery runs on that one dispatcher thread, which blocks on the queue and
 * hands each message to every subscriber in turn. Because a single thread drives
 * delivery, messages are processed in order and subscribers are never invoked
 * concurrently for the same topic. Failed deliveries are retried with linear
 * backoff before being dead-lettered (logged).
 *
 * <p>The supplied executor must be dedicated to this topic; the topic submits one
 * long-running loop task to it.
 *
 * @param <T> the type of message carried by this topic
 */
public final class Topic<T> {

    private static final Logger log = LoggerFactory.getLogger(Topic.class);

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MILLIS = 50L;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 1L;

    private final String name;
    private final BlockingQueue<T> queue;
    private final ExecutorService dispatcher;
    private final List<Subscriber<T>> subscribers = new CopyOnWriteArrayList<>();

    /**
     * Creates a topic and starts its dispatcher loop.
     *
     * @param name         the topic name; must not be {@code null}
     * @param queueFactory supplies this topic's buffer; invoked exactly once
     * @param dispatcher   a single-thread executor dedicated to draining this topic
     * @throws IllegalArgumentException if the supplied queue rejects its configuration
     *                                  (e.g. a non-positive capacity)
     */
    public Topic(String name, Supplier<BlockingQueue<T>> queueFactory, ExecutorService dispatcher) {
        this.name = Objects.requireNonNull(name, "name");
        this.queue = Objects.requireNonNull(queueFactory, "queueFactory").get();
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        startDispatcher();
    }

    /**
     * Publishes a message to this topic without blocking.
     *
     * <p>If the buffer is full the message is dropped and a warning is logged.
     *
     * @param message the message to publish; must not be {@code null}
     * @return {@code true} if the message was buffered, {@code false} if it was dropped
     */
    public boolean publish(T message) {
        Objects.requireNonNull(message, "message");
        boolean accepted = queue.offer(message);
        if (!accepted) {
            log.warn("Topic '{}' buffer full; message dropped", name);
        }
        return accepted;
    }

    /**
     * Registers a subscriber to receive all future messages on this topic.
     *
     * @param subscriber the subscriber to add; must not be {@code null}
     */
    public void addSubscriber(Subscriber<T> subscriber) {
        subscribers.add(Objects.requireNonNull(subscriber, "subscriber"));
    }

    private void startDispatcher() {
        dispatcher.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    T message = queue.take();
                    for (Subscriber<T> sub : subscribers) {
                        deliverWithRetry(sub, message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void deliverWithRetry(Subscriber<T> sub, T message) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (sub.onMessage(message)) {
                    return; // ACK
                }
            } catch (RuntimeException e) {
                log.warn("Subscriber '{}' threw on attempt {}/{}",
                        sub.getId(), attempt + 1, MAX_RETRIES, e);
            }
            try {
                Thread.sleep(BASE_BACKOFF_MILLIS * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.error("Message dead-lettered for subscriber '{}' on topic '{}'", sub.getId(), name);
    }

    /**
     * Stops the dispatcher, interrupting its loop and waiting briefly for it to
     * terminate. Any messages still buffered are discarded. Idempotent.
     */
    public void stop() {
        dispatcher.shutdownNow();
        try {
            dispatcher.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return the name of this topic
     */
    public String getName() {
        return name;
    }
}
