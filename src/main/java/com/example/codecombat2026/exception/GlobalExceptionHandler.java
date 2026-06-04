package com.example.codecombat2026.exception;

import com.example.codecombat2026.dto.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MessageResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(new MessageResponse(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * SSE-specific failures — connection drops, client disconnects, async timeouts.
     * These happen on text/event-stream responses where we can't write JSON back.
     * Return null so Spring just closes the connection silently.
     */
    @ExceptionHandler({
        AsyncRequestNotUsableException.class,
        org.springframework.web.context.request.async.AsyncRequestTimeoutException.class,
        IOException.class
    })
    public void handleAsyncRequestErrors(Exception ex, HttpServletRequest request) {
        // Don't try to write a response — connection is already broken
        log.debug("Async/IO error on {}: {}", request.getRequestURI(), ex.getMessage());
    }

    @ExceptionHandler(com.example.codecombat2026.controller.SubmissionController.SseAuthException.class)
    public ResponseEntity<Void> handleSseAuthException() {
        // 401, no body — SSE clients should retry by getting a fresh ticket.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(DuelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDuelNotFound(DuelNotFoundException ex) {
        log.debug("Duel not found: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "DUEL_NOT_FOUND");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuelForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleDuelForbidden(DuelForbiddenException ex) {
        log.debug("Duel forbidden: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "DUEL_FORBIDDEN");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DuelStateConflictException.class)
    public ResponseEntity<Map<String, Object>> handleDuelStateConflict(DuelStateConflictException ex) {
        log.debug("Duel state conflict {}: {}", ex.getCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getCode());
        Object payload = ex.getPayload();
        if (payload instanceof Map<?, ?> payloadMap) {
            for (Map.Entry<?, ?> entry : payloadMap.entrySet()) {
                if (entry.getKey() != null) {
                    body.put(entry.getKey().toString(), entry.getValue());
                }
            }
        } else if (payload != null) {
            body.put("details", payload);
        }
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProctoringNotFound(
            com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException ex) {
        log.debug("Proctoring resource not found: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "NOT_FOUND");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(com.example.codecombat2026.proctoring.exception.ProctoringForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleProctoringForbidden(
            com.example.codecombat2026.proctoring.exception.ProctoringForbiddenException ex) {
        log.debug("Proctoring forbidden {}: {}", ex.getCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getCode());
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(com.example.codecombat2026.proctoring.exception.ProctoringValidationException.class)
    public ResponseEntity<Map<String, Object>> handleProctoringValidation(
            com.example.codecombat2026.proctoring.exception.ProctoringValidationException ex) {
        log.debug("Proctoring validation error {}: {}", ex.getCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getCode());
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, ex.getHttpStatus());
    }

    @ExceptionHandler(com.example.codecombat2026.proctoring.exception.ProctoringStateConflictException.class)
    public ResponseEntity<Map<String, Object>> handleProctoringStateConflict(
            com.example.codecombat2026.proctoring.exception.ProctoringStateConflictException ex) {
        log.debug("Proctoring state conflict {}: {}", ex.getCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getCode());
        body.put("message", ex.getMessage());
        Object payload = ex.getPayload();
        if (payload instanceof Map<?, ?> payloadMap) {
            for (Map.Entry<?, ?> entry : payloadMap.entrySet()) {
                if (entry.getKey() != null) {
                    body.put(entry.getKey().toString(), entry.getValue());
                }
            }
        } else if (payload != null) {
            body.put("details", payload);
        }
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        // Let Spring's own ResponseStatusException flow through unchanged so
        // controllers can throw clean 401/403/etc. Only true unhandled errors
        // become 500s here.
        if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
            log.debug("ResponseStatusException on {}: {} {}", request.getRequestURI(),
                rse.getStatusCode(), rse.getReason());
            return new ResponseEntity<>(
                new MessageResponse(rse.getReason() != null ? rse.getReason() : ""),
                rse.getStatusCode());
        }

        // Skip SSE endpoints — Content-Type is text/event-stream, can't write JSON.
        // Browser closing tab / SSE timeout / connection drop all bubble up here.
        String uri = request.getRequestURI();
        if (uri != null && (uri.contains("/stream") || uri.endsWith("/sse"))) {
            log.debug("Suppressed exception on SSE endpoint {}: {}", uri, ex.getMessage());
            return null; // returning null signals Spring not to write a body
        }

        log.error("Unhandled exception on {}: {}", uri, ex.getMessage(), ex);
        return new ResponseEntity<>(new MessageResponse(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
