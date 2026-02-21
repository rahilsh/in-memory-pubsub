package service;
import interfaces.Subscriber;

public class ConsoleSubscriber<T> implements Subscriber<T> {
  private final String id;

  public ConsoleSubscriber(String id) { this.id = id; }

  @Override
  public boolean onMessage(T message) {
    System.out.println("[Console " + id + "] Received: " + message);
    return true;
  }

  @Override
  public String getId() { return id; }
}