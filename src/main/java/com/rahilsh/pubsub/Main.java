package com.rahilsh.pubsub;

import com.rahilsh.pubsub.core.PubSubBroker;
import com.rahilsh.pubsub.subscriber.ConsoleSubscriber;
import com.rahilsh.pubsub.subscriber.FileSubscriber;
import java.nio.file.Path;

/**
 * Runnable demonstration of the in-memory pub/sub system.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        PubSubBroker broker = new PubSubBroker();

        // 1. Create a topic with a buffer capacity of 10.
        broker.createTopic("demo", 10);

        // 2. Register subscribers of different types.
        broker.subscribe("demo", new ConsoleSubscriber<>("console-1"));
        broker.subscribe("demo", new FileSubscriber<>("file-1", Path.of("output.log")));

        // 3. Publish asynchronously.
        broker.publish("demo", "In-memory pub/sub is running!");
        broker.publish("demo", "This message goes to console AND file.");

        // 4. Give the dispatcher a moment to deliver, then shut down.
        Thread.sleep(200);
        broker.shutdown();
    }
}
