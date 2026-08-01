package com.rahilsh.pubsub.subscriber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubscriberTest {

    @Test
    void consoleSubscriberAlwaysAcknowledges() {
        ConsoleSubscriber<String> sub = new ConsoleSubscriber<>("c1");
        assertTrue(sub.onMessage("hello"));
        assertEquals("c1", sub.getId());
    }

    @Test
    void fileSubscriberWritesMessagesAndAcknowledges(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("out.log");
        FileSubscriber<String> sub = new FileSubscriber<>("f1", file);

        assertTrue(sub.onMessage("line-1"));
        assertTrue(sub.onMessage("line-2"));

        assertEquals("f1", sub.getId());
        assertEquals(List.of("line-1", "line-2"), Files.readAllLines(file));
    }

    @Test
    void fileSubscriberNacksOnIoFailure(@TempDir Path dir) {
        // Point at a path whose parent is a regular file, so opening it fails.
        Path notADir = dir.resolve("blocker");
        Path unwritable = notADir.resolve("child.log");
        try {
            Files.writeString(notADir, "x");
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        FileSubscriber<String> sub = new FileSubscriber<>("f2", unwritable);
        assertFalse(sub.onMessage("boom"));
    }
}
