package com.devpilot.exception;

import com.devpilot.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GitHubException.class)
    public ResponseEntity<ErrorResponse> handleGitHubException(GitHubException ex) {
        log.warn("GitHub operation failed: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .error("GITHUB_ERROR")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(AgentException.class)
    public ResponseEntity<ErrorResponse> handleAgentException(AgentException ex) {
        log.warn("DevPilot agent error: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .error("AI_AGENT_ERROR")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation error: {}", validationErrors);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .error("VALIDATION_ERROR")
                .message(validationErrors.isEmpty() ? "Invalid request payload" : validationErrors)
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .error("MALFORMED_REQUEST")
                .message("Request body is missing or malformed JSON")
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred while processing your request. Please try again.")
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
