# In-Memory Pub/Sub

[![CI](https://github.com/rahilsh/in-memory-pubsub/actions/workflows/maven.yml/badge.svg)](https://github.com/rahilsh/in-memory-pubsub/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)

A thread-safe, high-performance implementation of a Producer/Consumer messaging
system in Java — a compact low-level-design (LLD) reference for an in-memory
pub/sub broker.

<!-- git-ai exclusion test -->


## Features

- **Async processing** — producers are never blocked by consumer processing time.
- **Fault tolerance** — automatic retries with linear backoff for failing subscribers.
- **Thread safety** — built on `CopyOnWriteArrayList` and `BlockingQueue` for safe concurrent access.
- **Bounded backpressure** — each topic has a fixed buffer; overflow messages are dropped and logged.
- **Extensible** — add new subscriber types (file, SMS, DB, …) by implementing `Subscriber<T>`.

## Requirements

- JDK 21+
- Maven 3.9+

## Build & test

```bash
mvn clean verify
```

## Run the demo

```bash
mvn -q compile exec:java -Dexec.mainClass=com.rahilsh.pubsub.Main
```

Or run `com.rahilsh.pubsub.Main` from your IDE.

## Usage

```java
PubSubBroker broker = new PubSubBroker();

// Create a topic with a buffer capacity of 10.
broker.createTopic("orders", 10);

// Register subscribers.
broker.subscribe("orders", new ConsoleSubscriber<>("console-1"));
broker.subscribe("orders", new FileSubscriber<>("file-1", Path.of("orders.log")));

// Publish asynchronously.
broker.publish("orders", "order-42 created");

// Shut down when finished.
broker.shutdown();
```

### Custom subscribers

```java
public final class MySubscriber<T> implements Subscriber<T> {
    @Override
    public boolean onMessage(T message) {
        // return true to ACK, false to trigger a retry (NACK)
        return true;
    }

    @Override
    public String getId() {
        return "my-subscriber";
    }
}
```

## Design notes

### Concurrency model
The broker mints one dedicated single-thread executor per topic and injects it
(along with a `Supplier<BlockingQueue<T>>` for the buffer) into the `Topic`. Each
topic's dispatcher thread blocks on its queue and hands every message to each
subscriber in turn. Because a single thread drives delivery, messages are
processed in order and subscribers are never invoked concurrently for the same
topic. The queue implementation is injected, so a topic can be backed by a
bounded, unbounded, array-backed, or priority queue without changing `Topic`.

### Backpressure
Topic buffers are bounded. When full, `publish` returns `false` and the message is
dropped with a warning. Switching `offer()` to `put()` would instead block the
producer — a deliberate trade-off left to the caller's requirements.

### Reliability (ACK/NACK)
Delivery uses return-based acknowledgement. If a subscriber returns `false` (or
throws), the topic retries with linear backoff up to a fixed number of attempts
before dead-lettering the message (logged).

## Project layout

```
com.rahilsh.pubsub
├── api          # Subscriber interface
├── core         # Topic, PubSubBroker
└── subscriber   # ConsoleSubscriber, FileSubscriber
```

## Contributing

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) and our
[Code of Conduct](CODE_OF_CONDUCT.md).

## License

Distributed under the [MIT License](LICENSE).
