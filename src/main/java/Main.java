import service.PubSubBroker;
import service.ConsoleSubscriber;
import service.FileSubscriber;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    PubSubBroker broker = new PubSubBroker();

    // 1. Setup Topics
    broker.createTopic("interview-demo", 10);

    // 2. Register Different Subscriber Types
    broker.subscribe("interview-demo", new ConsoleSubscriber<>("Console-1"));
    broker.subscribe("interview-demo", new FileSubscriber<>("File-Log-1", "output.log"));

    // 3. Demonstrate Async Publishing
    System.out.println("Publishing messages...");
    broker.publish("interview-demo", "LLD implementation is running!");
    broker.publish("interview-demo", "This message goes to console AND file.");

    // 4. Give the background dispatcher a second to process, then shut down
    Thread.sleep(1000);
    broker.shutdown();
    System.out.println("Main execution finished.");
  }
}