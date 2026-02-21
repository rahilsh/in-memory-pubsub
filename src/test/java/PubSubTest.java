import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import model.Topic;
import interfaces.Subscriber;

class PubSubTest {

  @Test
  void testRetryLogic() throws InterruptedException {
    Topic<String> topic = new Topic<>("test-topic", 10);
    AtomicInteger attempts = new AtomicInteger(0);

    // A subscriber that fails twice then succeeds
    Subscriber<String> flakySub = new Subscriber<>() {
      public boolean onMessage(String msg) {
        return attempts.incrementAndGet() > 2;
      }
      public String getId() { return "flaky-1"; }
    };

    topic.addSubscriber(flakySub);
    topic.publish("Hello Retry");

    Thread.sleep(500); // Wait for async processing
    assertEquals(3, attempts.get(), "Should have attempted 3 times (2 fails + 1 success)");
  }
}