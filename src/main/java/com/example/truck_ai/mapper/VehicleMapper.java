package com.example.truck_ai.mapper;

import com.example.truck_ai.dto.VehicleResponse;
import com.example.truck_ai.entity.Vehicle;

public final class VehicleMapper {

    private VehicleMapper() {
    }

    public static VehicleResponse toResponse(Vehicle vehicle) {
        VehicleResponse response = new VehicleResponse();
        response.setVehicleId(vehicle.getVehicleId());
        response.setCurrentDriverName(vehicle.getCurrentDriverName());
        response.setFleetOwnerId(vehicle.getFleetOwnerId());
        return response;
    }
}
