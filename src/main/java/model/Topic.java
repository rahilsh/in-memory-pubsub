package model;

import interfaces.Subscriber;
import java.util.List;
import java.util.concurrent.*;

public class Topic<T> {
  private final String name;
  private final BlockingQueue<T> queue;
  private final List<Subscriber<T>> subscribers = new CopyOnWriteArrayList<>();
  private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();
  private final int MAX_RETRIES = 3;

  public Topic(String name, int capacity) {
    this.name = name;
    this.queue = new LinkedBlockingQueue<>(capacity);
    startDispatcher();
  }

  public void publish(T message) {
    if (!queue.offer(message)) {
      System.err.println("Topic " + name + " full. Message dropped.");
    }
  }

  public void addSubscriber(Subscriber<T> sub) {
    subscribers.add(sub);
  }

  private void startDispatcher() {
    dispatcher.submit(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          T message = queue.take();
          for (Subscriber<T> sub : subscribers) {
            retryWithBackoff(sub, message);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
  }

  public void stop() {
    dispatcher.shutdown();
    try {
      if (!dispatcher.awaitTermination(1, TimeUnit.SECONDS)) {
        dispatcher.shutdownNow();
      }
    } catch (InterruptedException ie) {
      dispatcher.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void retryWithBackoff(Subscriber<T> sub, T message) {
    for (int i = 0; i < MAX_RETRIES; i++) {
      if (sub.onMessage(message)) return; // ACK
      try { Thread.sleep(50L * (i + 1)); } catch (InterruptedException e) { break; }
    }
    System.err.println("Message DLQed for subscriber: " + sub.getId());
  }
}