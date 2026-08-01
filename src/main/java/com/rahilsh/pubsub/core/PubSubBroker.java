package com.rahilsh.pubsub.core;

import com.rahilsh.pubsub.api.Subscriber;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Entry point for creating topics, registering subscribers, and publishing
 * messages in the in-memory pub/sub system.
 *
 * <p>The broker owns dispatch: it mints one dedicated single-thread executor per
 * topic (via an injected {@link Supplier}) and injects it, along with a queue
 * factory, into each {@link Topic}. Both collaborators are injected, so tests and
 * advanced callers can supply their own execution and buffering strategies.
 *
 * <p>All operations are thread-safe. Topics are keyed by name.
 */
public final class PubSubBroker {

    private static final int DEFAULT_CAPACITY = 1024;

    private final Supplier<ExecutorService> dispatcherFactory;
    private final Supplier<BlockingQueue<Object>> defaultQueueFactory;
    private final Map<String, Topic<Object>> topics = new ConcurrentHashMap<>();

    /**
     * Creates a broker that mints daemon single-thread dispatchers and bounded
     * default buffers.
     */
    public PubSubBroker() {
        this(defaultDispatcherFactory(), () -> new LinkedBlockingQueue<>(DEFAULT_CAPACITY));
    }

    /**
     * Creates a broker with injected collaborators — the primary constructor for
     * testing and advanced configuration.
     *
     * @param dispatcherFactory   mints a fresh, dedicated executor for each topic
     * @param defaultQueueFactory supplies a buffer for topics created without an
     *                            explicit factory
     */
    public PubSubBroker(Supplier<ExecutorService> dispatcherFactory,
                        Supplier<BlockingQueue<Object>> defaultQueueFactory) {
        this.dispatcherFactory = Objects.requireNonNull(dispatcherFactory, "dispatcherFactory");
        this.defaultQueueFactory = Objects.requireNonNull(defaultQueueFactory, "defaultQueueFactory");
    }

    private static Supplier<ExecutorService> defaultDispatcherFactory() {
        AtomicInteger counter = new AtomicInteger();
        return () -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pubsub-topic-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Creates a topic using the broker's default queue factory, if absent.
     *
     * @param name the topic name
     */
    public void createTopic(String name) {
        Objects.requireNonNull(name, "name");
        topics.computeIfAbsent(name, n -> new Topic<>(n, defaultQueueFactory, dispatcherFactory.get()));
    }

    /**
     * Creates a topic backed by a bounded {@link LinkedBlockingQueue}, if absent.
     *
     * @param name     the topic name
     * @param capacity the buffer capacity; must be positive
     */
    public void createTopic(String name, int capacity) {
        Objects.requireNonNull(name, "name");
        topics.computeIfAbsent(name,
                n -> new Topic<>(n, () -> new LinkedBlockingQueue<>(capacity), dispatcherFactory.get()));
    }

    /**
     * Creates a topic with a caller-supplied queue factory, if absent. Use this to
     * plug in alternative buffer strategies (priority, array-backed, unbounded, …).
     *
     * @param name         the topic name
     * @param queueFactory supplies this topic's buffer
     */
    public void createTopic(String name, Supplier<BlockingQueue<Object>> queueFactory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(queueFactory, "queueFactory");
        topics.computeIfAbsent(name, n -> new Topic<>(n, queueFactory, dispatcherFactory.get()));
    }

    /**
     * Publishes a message to the named topic. No-op if the topic does not exist.
     *
     * @param topicName the target topic
     * @param message   the message payload; must not be {@code null}
     * @return {@code true} if the topic exists and accepted the message
     */
    public boolean publish(String topicName, Object message) {
        Topic<Object> topic = topics.get(topicName);
        return topic != null && topic.publish(message);
    }

    /**
     * Registers a subscriber on the named topic. No-op if the topic does not exist.
     *
     * @param topicName  the target topic
     * @param subscriber the subscriber to register
     */
    public void subscribe(String topicName, Subscriber<Object> subscriber) {
        Topic<Object> topic = topics.get(topicName);
        if (topic != null) {
            topic.addSubscriber(subscriber);
        }
    }

    /**
     * Stops every topic's dispatcher and releases resources. Idempotent.
     */
    public void shutdown() {
        topics.values().forEach(Topic::stop);
    }
}
