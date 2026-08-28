package com.tictactoe.engine.api;

import com.tictactoe.engine.service.GameAlreadyExistsException;
import com.tictactoe.engine.service.GameNotFoundException;
import com.tictactoe.engine.service.MoveRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> gameNotFound(GameNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "GAME_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(GameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> gameAlreadyExists(GameAlreadyExistsException ex) {
        return respond(HttpStatus.CONFLICT, "GAME_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(MoveRejectedException.class)
    public ResponseEntity<ErrorResponse> moveRejected(MoveRejectedException ex) {
        return respond(HttpStatus.CONFLICT, ex.reason().name(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", validationMessage(ex));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such resource");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }

    private static String validationMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException invalid) {
            String fields = invalid.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            return fields.isEmpty() ? "Invalid request" : fields;
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed request body";
        }
        return ex.getMessage() == null ? "Invalid request" : ex.getMessage();
    }

    /** Errors are always JSON, whatever the request said it accepts. */
    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(ErrorResponse.of(code, message));
    }
}
