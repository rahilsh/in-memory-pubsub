# In-Memory Pub-Sub

A thread-safe, high-performance LLD implementation of a Producer-Consumer messaging system.

## Features
- **Async Processing**: Producers are not blocked by consumer processing time.
- **Fault Tolerance**: Automatic retries with backoff for failing subscribers.
- **Thread Safety**: Uses `CopyOnWriteArrayList` and `BlockingQueue` for concurrent access.
- **Extensible**: Add new subscriber types (File, SMS, DB) by implementing `Subscriber<T>`.

# Low-Level Design Decisions

### 1. Concurrency Model
Used `LinkedBlockingQueue` for the message buffer. This handles the synchronization between the `publish` thread and the background `dispatcher` thread automatically.

### 2. Backpressure
The queue size is bounded. When full, the system currently drops messages (could be changed to `.put()` to block the producer, depending on requirements).

### 3. Reliability (ACK/NACK)
Implemented a return-based acknowledgement system. If a subscriber returns `false`, the topic enters a retry loop before moving the message to the internal error log (DLQ).