package com.tictactoe.session.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * The Engine understood the request and refused it (4xx) with a business error code.
 */
public class EngineRejectedException extends RuntimeException {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String code;

    public EngineRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static EngineRejectedException from(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String code = "ENGINE_" + status;
        try {
            EngineErrorResponse body = JSON.readValue(response.getBody(), EngineErrorResponse.class);
            if (body.code() != null) {
                code = body.code();
            }
        } catch (IOException unreadable) {
            // keep the synthetic code; the status is all we know
        }
        return new EngineRejectedException(code, "Engine rejected the request with " + status);
    }
}
