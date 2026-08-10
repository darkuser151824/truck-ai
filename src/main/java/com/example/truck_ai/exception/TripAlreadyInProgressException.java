package com.example.truck_ai.exception;

public class TripAlreadyInProgressException extends RuntimeException {

    public TripAlreadyInProgressException(Long vehicleId) {
        super("Trip already in progress for vehicleId=" + vehicleId + ", cannot be started");
    }
}
