package com.rahilsh.pubsub.subscriber;

import com.rahilsh.pubsub.api.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Subscriber} that logs every received message. Always acknowledges.
 *
 * @param <T> the type of message consumed
 */
public final class ConsoleSubscriber<T> implements Subscriber<T> {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSubscriber.class);

    private final String id;

    /**
     * @param id a stable identifier for this subscriber
     */
    public ConsoleSubscriber(String id) {
        this.id = id;
    }

    @Override
    public boolean onMessage(T message) {
        log.info("[{}] received: {}", id, message);
        return true;
    }

    @Override
    public String getId() {
        return id;
    }
}
