package com.example.truck_ai.exception;

import com.example.truck_ai.template.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Handled MethodArgumentNotValidException -> 400: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(DuplicateTripEventException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateTripEventException e) {
        log.warn("Handled DuplicateTripEventException -> 409: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(TripAlreadyInProgressException.class)
    public ResponseEntity<ApiResponse<Void>> handleTripAlreadyInProgress(TripAlreadyInProgressException e) {
        log.warn("Handled TripAlreadyInProgressException -> 409: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(InvalidTripTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTripTransition(InvalidTripTransitionException e) {
        log.warn("Handled InvalidTripTransitionException -> 409: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException e) {
        log.warn("Handled EntityNotFoundException -> 404: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    // Catch-all: any exception not handled above falls through here. The full
    // exception (with stack trace) is logged server-side only; the client
    // only ever gets a generic message so internals are never leaked.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        log.error("Handled unexpected exception -> 500", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later."));
    }
}
