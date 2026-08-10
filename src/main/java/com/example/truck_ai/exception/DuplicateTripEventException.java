package com.example.truck_ai.exception;

public class DuplicateTripEventException extends RuntimeException {

    public DuplicateTripEventException(String idempotencyKey) {
        super("Duplicate TripEvent rejected for idempotencyKey=" + idempotencyKey);
    }
}
