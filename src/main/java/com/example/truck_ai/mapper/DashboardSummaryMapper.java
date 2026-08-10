package com.example.truck_ai.mapper;

import com.example.truck_ai.dto.DashboardSummaryResponse;
import com.example.truck_ai.repository.TripRepository;

import java.util.List;
import java.util.stream.Collectors;

public final class DashboardSummaryMapper {

    private DashboardSummaryMapper() {
    }

    public static DashboardSummaryResponse toResponse(long totalTrips,
                                                        List<TripRepository.TripStatusCount> tripStatusCounts,
                                                        long totalEvents,
                                                        long lowConfidenceEventCount) {
        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setTotalTrips(totalTrips);
        response.setTripsByStatus(tripStatusCounts.stream()
                .collect(Collectors.toMap(TripRepository.TripStatusCount::getStatus, TripRepository.TripStatusCount::getCount)));
        response.setTotalEvents(totalEvents);
        response.setLowConfidenceEventCount(lowConfidenceEventCount);
        return response;
    }
}
