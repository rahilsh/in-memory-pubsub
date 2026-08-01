package com.rahilsh.pubsub.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rahilsh.pubsub.api.Subscriber;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PubSubBrokerTest {

    private PubSubBroker broker;

    private static PubSubBroker newBroker() {
        Supplier<ExecutorService> dispatchers = () -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        Supplier<BlockingQueue<Object>> queues = () -> new LinkedBlockingQueue<>(16);
        return new PubSubBroker(dispatchers, queues);
    }

    @AfterEach
    void tearDown() {
        if (broker != null) {
            broker.shutdown();
        }
    }

    @Test
    void deliversPublishedMessageToSubscriber() throws InterruptedException {
        broker = newBroker();
        broker.createTopic("orders", 10);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> received = new CopyOnWriteArrayList<>();
        broker.subscribe("orders", recording(received, latch));

        assertTrue(broker.publish("orders", "o1"));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("o1"), received);
    }

    @Test
    void createTopicWithDefaultQueueFactory() throws InterruptedException {
        broker = newBroker();
        broker.createTopic("defaulted");
        CountDownLatch latch = new CountDownLatch(1);
        broker.subscribe("defaulted", recording(new CopyOnWriteArrayList<>(), latch));

        broker.publish("defaulted", "x");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void createTopicWithCustomQueueFactory() throws InterruptedException {
        broker = newBroker();
        broker.createTopic("custom", () -> new LinkedBlockingQueue<>(4));
        CountDownLatch latch = new CountDownLatch(1);
        broker.subscribe("custom", recording(new CopyOnWriteArrayList<>(), latch));

        broker.publish("custom", "x");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void createTopicIsIdempotent() throws InterruptedException {
        broker = newBroker();
        broker.createTopic("dup", 10);
        CountDownLatch latch = new CountDownLatch(1);
        broker.subscribe("dup", recording(new CopyOnWriteArrayList<>(), latch));

        broker.createTopic("dup", 10); // must not replace the existing topic/subscriber

        broker.publish("dup", "x");
        assertTrue(latch.await(2, TimeUnit.SECONDS), "existing subscriber should survive re-create");
    }

    @Test
    void publishToUnknownTopicReturnsFalse() {
        broker = newBroker();
        assertFalse(broker.publish("missing", "x"));
    }

    @Test
    void subscribeToUnknownTopicIsNoOp() {
        broker = newBroker();
        // Must not throw.
        broker.subscribe("missing", recording(new CopyOnWriteArrayList<>(), new CountDownLatch(1)));
    }

    @Test
    void shutdownIsIdempotent() {
        broker = newBroker();
        broker.createTopic("a", 4);
        broker.shutdown();
        broker.shutdown(); // second call must not throw
        broker = null; // already shut down
    }

    @Test
    void defaultConstructorWorksEndToEnd() throws InterruptedException {
        broker = new PubSubBroker();
        broker.createTopic("real", 10);
        CountDownLatch latch = new CountDownLatch(1);
        broker.subscribe("real", recording(new CopyOnWriteArrayList<>(), latch));

        broker.publish("real", "x");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new PubSubBroker(null, () -> new LinkedBlockingQueue<>()));
        assertThrows(NullPointerException.class,
                () -> new PubSubBroker(() -> Executors.newSingleThreadExecutor(), null));

        broker = newBroker();
        assertThrows(NullPointerException.class, () -> broker.createTopic(null));
        assertThrows(NullPointerException.class, () -> broker.createTopic(null, 10));
        assertThrows(NullPointerException.class, () -> broker.createTopic("t", (Supplier<BlockingQueue<Object>>) null));
        assertThrows(NullPointerException.class, () -> broker.createTopic(null, () -> new LinkedBlockingQueue<>()));
    }

    private static Subscriber<Object> recording(List<String> sink, CountDownLatch latch) {
        return new Subscriber<>() {
            @Override
            public boolean onMessage(Object msg) {
                sink.add(String.valueOf(msg));
                latch.countDown();
                return true;
            }

            @Override
            public String getId() {
                return "rec";
            }
        };
    }
}
