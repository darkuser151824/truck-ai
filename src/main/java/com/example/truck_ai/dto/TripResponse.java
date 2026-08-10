package com.example.truck_ai.dto;

import com.example.truck_ai.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private Long tripId;
    private Long vehicleId;
    private String driverName;
    private Long fleetOwnerId;
    private TripStatus status;
    private String originPlace;
    private String destinationPlace;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
