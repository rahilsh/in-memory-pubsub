package com.rahilsh.pubsub.subscriber;

import com.rahilsh.pubsub.api.Subscriber;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Subscriber} that appends every received message to a file.
 *
 * <p>NACKs (returns {@code false}) on I/O failure so the topic can retry.
 *
 * @param <T> the type of message consumed
 */
public final class FileSubscriber<T> implements Subscriber<T> {

    private static final Logger log = LoggerFactory.getLogger(FileSubscriber.class);

    private final String id;
    private final Path path;

    /**
     * @param id   a stable identifier for this subscriber
     * @param path the file to append messages to
     */
    public FileSubscriber(String id, Path path) {
        this.id = id;
        this.path = path;
    }

    @Override
    public boolean onMessage(T message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(path.toFile(), true))) {
            out.println(message);
            return true;
        } catch (IOException e) {
            log.warn("[{}] failed to write to {}", id, path, e);
            return false;
        }
    }

    @Override
    public String getId() {
        return id;
    }
}
