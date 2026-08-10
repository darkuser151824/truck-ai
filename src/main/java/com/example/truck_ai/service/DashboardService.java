package com.example.truck_ai.service;

import com.example.truck_ai.dto.DashboardSummaryResponse;
import com.example.truck_ai.mapper.DashboardSummaryMapper;
import com.example.truck_ai.repository.TripEventRepository;
import com.example.truck_ai.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.8;

    private final TripRepository tripRepository;
    private final TripEventRepository tripEventRepository;

    public DashboardSummaryResponse getSummary() {
        log.debug("getSummary() start");
        long totalTrips = tripRepository.count();
        long totalEvents = tripEventRepository.count();
        long lowConfidenceEventCount = tripEventRepository.countByConfidenceLessThan(LOW_CONFIDENCE_THRESHOLD);
        DashboardSummaryResponse response = DashboardSummaryMapper.toResponse(
                totalTrips, tripRepository.countTripsByStatus(), totalEvents, lowConfidenceEventCount);
        log.info("getSummary(): totalTrips={}, totalEvents={}, lowConfidenceEventCount={}",
                totalTrips, totalEvents, lowConfidenceEventCount);
        return response;
    }
}
