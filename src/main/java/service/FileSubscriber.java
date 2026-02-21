package service;

import interfaces.Subscriber;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileSubscriber<T> implements Subscriber<T> {
  private final String id;
  private final String path;

  public FileSubscriber(String id, String path) {
    this.id = id;
    this.path = path;
  }

  @Override
  public boolean onMessage(T message) {
    try (PrintWriter out = new PrintWriter(new FileWriter(path, true))) {
      out.println(message);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public String getId() { return id; }
}