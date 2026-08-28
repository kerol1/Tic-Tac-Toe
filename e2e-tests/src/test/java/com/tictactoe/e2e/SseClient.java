package com.tictactoe.e2e;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Reads named server-sent events on a background thread; tests wait, never sleep. */
final class SseClient implements AutoCloseable {

    record Event(String name, String data) {
    }

    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Thread reader;

    SseClient(String url) {
        reader = Thread.ofVirtual().start(() -> consume(url));
    }

    Event next(Duration timeout) throws InterruptedException {
        Event event = events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (event == null) {
            throw new AssertionError("No SSE event within " + timeout);
        }
        return event;
    }

    /** Drains events until one with the given name arrives. */
    Event nextNamed(String name, Duration timeout) throws InterruptedException {
        Event event = next(timeout);
        while (!event.name().equals(name)) {
            event = next(timeout);
        }
        return event;
    }

    private void consume(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Accept", "text/event-stream").GET().build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader lines = new BufferedReader(new InputStreamReader(response.body()))) {
                String name = null;
                StringBuilder data = new StringBuilder();
                String line;
                while ((line = lines.readLine()) != null) {
                    if (line.isEmpty()) {
                        if (name != null) {
                            events.add(new Event(name, data.toString()));
                        }
                        name = null;
                        data.setLength(0);
                    } else if (line.startsWith("event:")) {
                        name = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        data.append(line.substring(5).trim());
                    }
                }
            }
        } catch (IOException | InterruptedException ignored) {
            // stream closed; whatever was queued is still readable
        }
    }

    @Override
    public void close() {
        reader.interrupt();
        client.close();
    }
}
