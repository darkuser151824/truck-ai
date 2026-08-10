package com.example.truck_ai.controller;

import com.example.truck_ai.dto.TripResponse;
import com.example.truck_ai.dto.VehicleRequest;
import com.example.truck_ai.dto.VehicleResponse;
import com.example.truck_ai.service.TripService;
import com.example.truck_ai.service.VehicleService;
import com.example.truck_ai.template.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final TripService tripService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(@Valid @RequestBody VehicleRequest request) {
        log.info("HTTP POST /api/vehicles: driver={}, fleetOwnerId={}", request.getCurrentDriverName(), request.getFleetOwnerId());
        VehicleResponse saved = vehicleService.createVehicle(request);
        log.info("HTTP POST /api/vehicles: 201 vehicleId={}", saved.getVehicleId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Vehicle created successfully", saved));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable Long vehicleId) {
        log.info("HTTP GET /api/vehicles/{}", vehicleId);
        VehicleResponse vehicle = vehicleService.getVehicleById(vehicleId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Vehicle fetched successfully", vehicle));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAllVehicles() {
        log.info("HTTP GET /api/vehicles");
        List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Vehicles fetched successfully", vehicles));
    }

    @GetMapping("/{vehicleId}/trips")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getVehicleTrips(@PathVariable Long vehicleId) {
        log.info("HTTP GET /api/vehicles/{}/trips", vehicleId);
        List<TripResponse> trips = tripService.getTripsByVehicleId(vehicleId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Vehicle trips fetched successfully", trips));
    }
}
