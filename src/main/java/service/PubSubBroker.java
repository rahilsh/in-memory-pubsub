package service;

import model.Topic;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PubSubBroker {
  private final Map<String, Topic<Object>> topics = new ConcurrentHashMap<>();

  public void createTopic(String name, int capacity) {
    topics.putIfAbsent(name, new Topic<>(name, capacity));
  }

  public void publish(String topicName, Object message) {
    Topic<Object> topic = topics.get(topicName);
    if (topic != null) topic.publish(message);
  }

  public void shutdown() {
    topics.values().forEach(Topic::stop);
  }

  public void subscribe(String topicName, interfaces.Subscriber<Object> sub) {
    Topic<Object> topic = topics.get(topicName);
    if (topic != null) topic.addSubscriber(sub);
  }
}