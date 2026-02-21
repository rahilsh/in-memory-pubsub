package interfaces;

public interface Subscriber<T> {
  boolean onMessage(T message); // Returns true for ACK, false for NACK
  String getId();
}