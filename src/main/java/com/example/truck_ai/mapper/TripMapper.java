package com.example.truck_ai.mapper;

import com.example.truck_ai.dto.TripResponse;
import com.example.truck_ai.entity.Trip;

public final class TripMapper {

    private TripMapper() {
    }

    public static TripResponse toResponse(Trip trip) {
        TripResponse response = new TripResponse();
        response.setTripId(trip.getTripId());
        response.setVehicleId(trip.getVehicle() != null ? trip.getVehicle().getVehicleId() : null);
        response.setDriverName(trip.getVehicle() != null ? trip.getVehicle().getCurrentDriverName() : null);
        response.setFleetOwnerId(trip.getVehicle() != null ? trip.getVehicle().getFleetOwnerId() : null);
        response.setStatus(trip.getStatus());
        response.setOriginPlace(trip.getOriginPlace());
        response.setDestinationPlace(trip.getDestinationPlace());
        response.setStartedAt(trip.getStartedAt());
        response.setCompletedAt(trip.getCompletedAt());
        return response;
    }
}
