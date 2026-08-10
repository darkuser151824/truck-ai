package com.example.truck_ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private Long vehicleId;
    private String currentDriverName;
    private Long fleetOwnerId;
}
