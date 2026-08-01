package com.rahilsh.pubsub.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rahilsh.pubsub.api.Subscriber;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TopicTest {

    private final List<Topic<?>> created = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        created.forEach(Topic::stop);
    }

    private Topic<String> newTopic(int capacity) {
        return newTopic(capacity, Executors.newSingleThreadExecutor(daemon()));
    }

    private Topic<String> newTopic(int capacity, ExecutorService dispatcher) {
        Topic<String> topic = new Topic<>("t", bounded(capacity), dispatcher);
        created.add(topic);
        return topic;
    }

    private static java.util.concurrent.ThreadFactory daemon() {
        return r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };
    }

    private static <T> Supplier<BlockingQueue<T>> bounded(int capacity) {
        return () -> new LinkedBlockingQueue<>(capacity);
    }

    @Test
    void deliversMessageToSubscriber() throws InterruptedException {
        Topic<String> topic = newTopic(10);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> received = new CopyOnWriteArrayList<>();
        topic.addSubscriber(recording("s", received, latch));

        topic.publish("hello");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("hello"), received);
    }

    @Test
    void fansOutEveryMessageToEverySubscriber() throws InterruptedException {
        Topic<String> topic = newTopic(10);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        topic.addSubscriber(counting("a", a, latch));
        topic.addSubscriber(counting("b", b, latch));

        topic.publish("m");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, a.get());
        assertEquals(1, b.get());
    }

    @Test
    void preservesPublishOrder() throws InterruptedException {
        Topic<String> topic = newTopic(10);
        CountDownLatch latch = new CountDownLatch(5);
        List<String> received = new CopyOnWriteArrayList<>();
        topic.addSubscriber(recording("s", received, latch));

        for (int i = 0; i < 5; i++) {
            topic.publish("m" + i);
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("m0", "m1", "m2", "m3", "m4"), received);
    }

    @Test
    void retriesUntilSubscriberAcknowledges() throws InterruptedException {
        Topic<String> topic = newTopic(10);
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch acked = new CountDownLatch(1);

        topic.addSubscriber(new Subscriber<>() {
            @Override
            public boolean onMessage(String msg) {
                boolean ack = attempts.incrementAndGet() > 2; // fail twice, then ACK
                if (ack) {
                    acked.countDown();
                }
                return ack;
            }

            @Override
            public String getId() {
                return "flaky";
            }
        });

        topic.publish("x");

        assertTrue(acked.await(2, TimeUnit.SECONDS));
        assertEquals(3, attempts.get());
    }

    @Test
    void deadLettersAfterMaxRetries() throws InterruptedException {
        Topic<String> topic = newTopic(10);
        CountDownLatch attempts = new CountDownLatch(3); // MAX_RETRIES
        AtomicInteger count = new AtomicInteger();

        topic.addSubscriber(new Subscriber<>() {
            @Override
            public boolean onMessage(String msg) {
                count.incrementAndGet();
                attempts.countDown();
                return false; // never acknowledges
            }

            @Override
            public String getId() {
                return "always-fail";
            }
        });

        topic.publish("x");

        assertTrue(attempts.await(2, TimeUnit.SECONDS), "should attempt exactly MAX_RETRIES times");
        Thread.sleep(100); // ensure no 4th attempt happens
        assertEquals(3, count.get());
    }

    @Test
    void recoversWhenSubscriberThrows() throws InterruptedException {
        Topic<String> topic = newTopic(10);
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch acked = new CountDownLatch(1);

        topic.addSubscriber(new Subscriber<>() {
            @Override
            public boolean onMessage(String msg) {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("boom");
                }
                acked.countDown();
                return true;
            }

            @Override
            public String getId() {
                return "throwing";
            }
        });

        topic.publish("x");

        assertTrue(acked.await(2, TimeUnit.SECONDS));
        assertEquals(2, attempts.get());
    }

    @Test
    void dropsMessagesWhenBufferIsFull() {
        // A dispatcher that never runs the loop, so the bounded buffer overflows.
        Topic<String> topic = newTopic(1, new DiscardingExecutorService());
        assertTrue(topic.publish("first"), "first message should fit");
        assertFalse(topic.publish("second"), "second message should be dropped when full");
    }

    @Test
    void publishRejectsNull() {
        Topic<String> topic = newTopic(10);
        assertThrows(NullPointerException.class, () -> topic.publish(null));
    }

    @Test
    void addSubscriberRejectsNull() {
        Topic<String> topic = newTopic(10);
        assertThrows(NullPointerException.class, () -> topic.addSubscriber(null));
    }

    @Test
    void constructorRejectsNullArguments() {
        ExecutorService exec = new DiscardingExecutorService();
        assertThrows(NullPointerException.class, () -> new Topic<>(null, bounded(1), exec));
        assertThrows(NullPointerException.class, () -> new Topic<String>("t", null, exec));
        assertThrows(NullPointerException.class, () -> new Topic<>("t", bounded(1), null));
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        ExecutorService exec = new DiscardingExecutorService();
        assertThrows(IllegalArgumentException.class, () -> new Topic<>("t", bounded(0), exec));
    }

    @Test
    void exposesName() {
        Topic<String> topic = new Topic<>("orders", bounded(1), new DiscardingExecutorService());
        assertEquals("orders", topic.getName());
    }

    @Test
    void stopIsIdempotent() {
        Topic<String> topic = new Topic<>("t", bounded(1), Executors.newSingleThreadExecutor(daemon()));
        topic.stop();
        topic.stop(); // second call must not throw
    }

    private static Subscriber<String> recording(String id, List<String> sink, CountDownLatch latch) {
        return new Subscriber<>() {
            @Override
            public boolean onMessage(String msg) {
                sink.add(msg);
                latch.countDown();
                return true;
            }

            @Override
            public String getId() {
                return id;
            }
        };
    }

    private static Subscriber<String> counting(String id, AtomicInteger counter, CountDownLatch latch) {
        return new Subscriber<>() {
            @Override
            public boolean onMessage(String msg) {
                counter.incrementAndGet();
                latch.countDown();
                return true;
            }

            @Override
            public String getId() {
                return id;
            }
        };
    }

    /** An {@link ExecutorService} that silently discards every submitted task. */
    private static final class DiscardingExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void execute(Runnable command) {
            // discard
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}
