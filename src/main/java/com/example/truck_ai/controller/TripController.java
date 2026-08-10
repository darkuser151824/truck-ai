package com.example.truck_ai.controller;

import com.example.truck_ai.dto.PageResponse;
import com.example.truck_ai.dto.TripEventResponse;
import com.example.truck_ai.dto.TripRequest;
import com.example.truck_ai.dto.TripResponse;
import com.example.truck_ai.enums.TripStatus;
import com.example.truck_ai.service.TripEventService;
import com.example.truck_ai.service.TripService;
import com.example.truck_ai.template.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final TripEventService tripEventService;

    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody TripRequest request) {
        log.info("HTTP POST /api/trips: vehicleId={}, origin={}, destination={}",
                request.getVehicleId(), request.getOriginPlace(), request.getDestinationPlace());
        TripResponse saved = tripService.createTrip(request);
        log.info("HTTP POST /api/trips: 201 tripId={}", saved.getTripId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Trip created successfully", saved));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable Long tripId) {
        log.info("HTTP GET /api/trips/{}", tripId);
        TripResponse trip = tripService.getTripById(tripId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trip fetched successfully", trip));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TripResponse>>> getAllTrips() {
        log.info("HTTP GET /api/trips");
        List<TripResponse> trips = tripService.getAllTrips();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trips fetched successfully", trips));
    }

    @GetMapping("/active/{vehicleId}")
    public ResponseEntity<ApiResponse<TripResponse>> getActiveTrip(@PathVariable Long vehicleId) {
        log.info("HTTP GET /api/trips/active/{}", vehicleId);
        TripResponse trip = tripService.getActiveTripByVehicleId(vehicleId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Active trip fetched successfully", trip));
    }

    @GetMapping(params = "status")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByStatus(@RequestParam TripStatus status) {
        log.info("HTTP GET /api/trips?status={}", status);
        List<TripResponse> trips = tripService.getTripsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trips fetched successfully", trips));
    }

    @GetMapping("/{tripId}/events")
    public ResponseEntity<ApiResponse<PageResponse<TripEventResponse>>> getTripEvents(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("HTTP GET /api/trips/{}/events: page={}, size={}", tripId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<TripEventResponse> events = tripEventService.getEventsByTripIdSortedDesc(tripId, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Trip events fetched successfully", events));
    }
}
