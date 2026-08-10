package com.example.truck_ai.service;

import com.example.truck_ai.dto.VehicleRequest;
import com.example.truck_ai.dto.VehicleResponse;
import com.example.truck_ai.entity.Vehicle;
import com.example.truck_ai.mapper.VehicleMapper;
import com.example.truck_ai.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleResponse createVehicle(VehicleRequest request) {
        log.debug("createVehicle() start: driver={}, fleetOwnerId={}", request.getCurrentDriverName(), request.getFleetOwnerId());

        Vehicle vehicle = Vehicle.builder()
                .currentDriverName(request.getCurrentDriverName())
                .fleetOwnerId(request.getFleetOwnerId())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("createVehicle(): saved vehicleId={}", saved.getVehicleId());
        return VehicleMapper.toResponse(saved);
    }

    public VehicleResponse getVehicleById(Long vehicleId) {
        log.debug("getVehicleById() start: vehicleId={}", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> {
                    log.warn("getVehicleById() rejected: no Vehicle found for vehicleId={}", vehicleId);
                    return new EntityNotFoundException("No vehicle found for vehicleId=" + vehicleId);
                });
        return VehicleMapper.toResponse(vehicle);
    }

    public List<VehicleResponse> getAllVehicles() {
        log.debug("getAllVehicles() start");
        List<Vehicle> vehicles = vehicleRepository.findAll();
        log.info("getAllVehicles(): found {} vehicles", vehicles.size());
        return vehicles.stream().map(VehicleMapper::toResponse).toList();
    }
}
