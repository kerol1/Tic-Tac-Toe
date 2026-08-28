package com.tictactoe.session.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Minimal SSE consumer: connects, parses named events on a background thread and hands
 * them out through blocking waits so tests never sleep.
 */
public class SseTestClient implements AutoCloseable {

    public record Event(String name, String id, String data) {
    }

    private static final Event STREAM_CLOSED = new Event("__closed__", null, null);

    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    private final CountDownLatch connected = new CountDownLatch(1);
    private final HttpClient client = HttpClient.newHttpClient();
    private final Thread reader;
    private volatile int statusCode;

    public SseTestClient(String url) {
        reader = Thread.ofVirtual().start(() -> consume(url));
    }

    public int awaitConnected(Duration timeout) throws InterruptedException {
        if (!connected.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new AssertionError("SSE stream did not connect within " + timeout);
        }
        return statusCode;
    }

    /** Next event, or {@code null} if none arrives in time. */
    public Event poll(Duration timeout) throws InterruptedException {
        return events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public Event next(Duration timeout) throws InterruptedException {
        Event event = poll(timeout);
        if (event == null) {
            throw new AssertionError("No SSE event within " + timeout);
        }
        return event;
    }

    /** Drains events until the stream closes or a terminal event name is seen. */
    public List<Event> collectUntilClosed(Duration timeout) throws InterruptedException {
        List<Event> collected = new ArrayList<>();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new AssertionError("SSE stream did not close within " + timeout + "; got " + collected);
            }
            Event event = events.poll(remaining, TimeUnit.NANOSECONDS);
            if (event == null || event == STREAM_CLOSED) {
                return collected;
            }
            collected.add(event);
        }
    }

    public boolean isClosed() {
        return events.peek() == STREAM_CLOSED;
    }

    private void consume(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Accept", "text/event-stream").GET().build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            statusCode = response.statusCode();
            connected.countDown();
            try (BufferedReader lines = new BufferedReader(new InputStreamReader(response.body()))) {
                String name = null;
                String id = null;
                StringBuilder data = new StringBuilder();
                String line;
                while ((line = lines.readLine()) != null) {
                    if (line.isEmpty()) {
                        if (name != null || !data.isEmpty()) {
                            events.add(new Event(name == null ? "message" : name, id, data.toString()));
                        }
                        name = null;
                        id = null;
                        data.setLength(0);
                    } else if (line.startsWith("event:")) {
                        name = line.substring(6).trim();
                    } else if (line.startsWith("id:")) {
                        id = line.substring(3).trim();
                    } else if (line.startsWith("data:")) {
                        data.append(line.substring(5).trim());
                    }
                }
            }
        } catch (IOException | InterruptedException ex) {
            connected.countDown();
        } finally {
            events.add(STREAM_CLOSED);
        }
    }

    @Override
    public void close() {
        reader.interrupt();
        client.close();
    }
}
