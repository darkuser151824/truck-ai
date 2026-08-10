package com.example.truck_ai.controller;

import com.example.truck_ai.dto.PageResponse;
import com.example.truck_ai.dto.TripEventResponse;
import com.example.truck_ai.enums.EventType;
import com.example.truck_ai.service.TripEventService;
import com.example.truck_ai.template.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class TripEventController {

    private final TripEventService tripEventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<TripEventResponse>> getEvent(@PathVariable Long eventId) {
        log.info("HTTP GET /api/events/{}", eventId);
        TripEventResponse event = tripEventService.getEventById(eventId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trip event fetched successfully", event));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TripEventResponse>>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("HTTP GET /api/events: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<TripEventResponse> events = tripEventService.getAllEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trip events fetched successfully", events));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<List<TripEventResponse>>> getEventsByTrip(@PathVariable Long tripId) {
        log.info("HTTP GET /api/events/trip/{}", tripId);
        List<TripEventResponse> events = tripEventService.getEventsByTripId(tripId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trip events fetched successfully", events));
    }

    @GetMapping(params = "eventType")
    public ResponseEntity<ApiResponse<PageResponse<TripEventResponse>>> getEventsByType(
            @RequestParam EventType eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("HTTP GET /api/events?eventType={}: page={}, size={}", eventType, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<TripEventResponse> events = tripEventService.getEventsByType(eventType, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trip events fetched successfully", events));
    }

    @GetMapping(params = "maxConfidence")
    public ResponseEntity<ApiResponse<PageResponse<TripEventResponse>>> getEventsByMaxConfidence(
            @RequestParam Double maxConfidence,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("HTTP GET /api/events?maxConfidence={}: page={}, size={}", maxConfidence, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<TripEventResponse> events = tripEventService.getEventsByMaxConfidence(maxConfidence, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trip events fetched successfully", events));
    }
}
