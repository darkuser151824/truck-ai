package com.example.truck_ai.dto;

import com.example.truck_ai.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalTrips;
    private Map<TripStatus, Long> tripsByStatus;
    private long totalEvents;
    private long lowConfidenceEventCount;
}
