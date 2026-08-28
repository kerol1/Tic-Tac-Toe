package com.tictactoe.session.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record EngineErrorResponse(String code, String message) {
}
