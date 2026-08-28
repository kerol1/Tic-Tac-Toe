package com.tictactoe.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class Http {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    private Http() {
    }

    record Reply(int status, JsonNode body) {
    }

    static Reply post(String url) {
        return exchange(HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json").POST(HttpRequest.BodyPublishers.noBody()).build());
    }

    static Reply get(String url) {
        return exchange(HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json").GET().build());
    }

    private static Reply exchange(HttpRequest request) {
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            String text = response.body();
            return new Reply(response.statusCode(), text == null || text.isBlank() ? JSON.nullNode() : JSON.readTree(text));
        } catch (IOException ex) {
            throw new IllegalStateException("HTTP call failed: " + request.uri(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling " + request.uri(), ex);
        }
    }
}
