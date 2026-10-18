package com.brianngo333.news.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleStatus(ResponseStatusException ex) {
        return org.springframework.http.ResponseEntity.status(ex.getStatusCode()).body(new ApiError(ex.getStatusCode().value(), ex.getReason(), Instant.now()));
    }
    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(500, "Internal server error", Instant.now()));
    }
    public record ApiError(int status, String message, Instant timestamp) { }
}
