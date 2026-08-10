package com.example.truck_ai.exception;

import com.example.truck_ai.enums.TripStatus;

public class InvalidTripTransitionException extends RuntimeException {

    public InvalidTripTransitionException(Long tripId, TripStatus currentStatus, TripStatus targetStatus) {
        super("Cannot transition tripId=" + tripId + " from " + currentStatus + " to " + targetStatus);
    }
}
