package com.example.truck_ai.service;

import com.example.truck_ai.dto.TripRequest;
import com.example.truck_ai.dto.TripResponse;
import com.example.truck_ai.entity.Trip;
import com.example.truck_ai.entity.Vehicle;
import com.example.truck_ai.enums.TripStatus;
import com.example.truck_ai.exception.TripAlreadyInProgressException;
import com.example.truck_ai.mapper.TripMapper;
import com.example.truck_ai.repository.TripRepository;
import com.example.truck_ai.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private static final List<TripStatus> INACTIVE_STATUSES = List.of(TripStatus.COMPLETED, TripStatus.CANCELLED);

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    public TripResponse createTrip(TripRequest request) {
        log.debug("createTrip() start: vehicleId={}, origin={}, destination={}",
                request.getVehicleId(), request.getOriginPlace(), request.getDestinationPlace());

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> {
                    log.warn("createTrip() rejected: no Vehicle found for vehicleId={}", request.getVehicleId());
                    return new EntityNotFoundException("No vehicle found for vehicleId=" + request.getVehicleId());
                });

        // Pre-check first (cheap, avoids a failed insert in the common case);
        // the DB's partial unique index (uq_vehicle_active_trip) is the real
        // guarantee and is caught below to close the race between two
        // concurrent create requests for the same vehicle.
        if (tripRepository.findActiveTripByVehicleId(request.getVehicleId(), INACTIVE_STATUSES).isPresent()) {
            log.warn("createTrip() rejected: an active trip already exists for vehicleId={}", request.getVehicleId());
            throw new TripAlreadyInProgressException(request.getVehicleId());
        }

        Trip trip = Trip.builder()
                .vehicle(vehicle)
                .status(TripStatus.CREATED)
                .originPlace(request.getOriginPlace())
                .destinationPlace(request.getDestinationPlace())
                .startedAt(request.getStartedAt() != null ? request.getStartedAt() : LocalDateTime.now())
                .build();
        log.trace("createTrip(): built Trip (pre-save): origin={}, destination={}", trip.getOriginPlace(), trip.getDestinationPlace());

        try {
            Trip saved = tripRepository.save(trip);
            log.info("createTrip(): saved tripId={} for vehicleId={}", saved.getTripId(), request.getVehicleId());
            return TripMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("createTrip() rejected: DB constraint caught a race-condition duplicate active trip for vehicleId={}", request.getVehicleId());
            throw new TripAlreadyInProgressException(request.getVehicleId());
        }
    }

    public TripResponse getTripById(Long tripId) {
        log.debug("getTripById() start: tripId={}", tripId);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> {
                    log.warn("getTripById() rejected: no Trip found for tripId={}", tripId);
                    return new EntityNotFoundException("No trip found for tripId=" + tripId);
                });
        return TripMapper.toResponse(trip);
    }

    public TripResponse getActiveTripByVehicleId(Long vehicleId) {
        log.debug("getActiveTripByVehicleId() start: vehicleId={}", vehicleId);
        Trip trip = tripRepository.findActiveTripByVehicleId(vehicleId, INACTIVE_STATUSES)
                .orElseThrow(() -> {
                    log.warn("getActiveTripByVehicleId() rejected: no active Trip found for vehicleId={}", vehicleId);
                    return new EntityNotFoundException("No active trip found for vehicleId=" + vehicleId);
                });
        return TripMapper.toResponse(trip);
    }

    public List<TripResponse> getAllTrips() {
        log.debug("getAllTrips() start");
        List<Trip> trips = tripRepository.findAll();
        log.info("getAllTrips(): found {} trips", trips.size());
        return trips.stream().map(TripMapper::toResponse).toList();
    }

    public List<TripResponse> getTripsByStatus(TripStatus status) {
        log.debug("getTripsByStatus() start: status={}", status);
        List<Trip> trips = tripRepository.findByStatus(status);
        log.info("getTripsByStatus(): found {} trips for status={}", trips.size(), status);
        return trips.stream().map(TripMapper::toResponse).toList();
    }

    public List<TripResponse> getTripsByVehicleId(Long vehicleId) {
        log.debug("getTripsByVehicleId() start: vehicleId={}", vehicleId);
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> {
                    log.warn("getTripsByVehicleId() rejected: no Vehicle found for vehicleId={}", vehicleId);
                    return new EntityNotFoundException("No vehicle found for vehicleId=" + vehicleId);
                });
        List<Trip> trips = tripRepository.findByVehicle_VehicleIdOrderByStartedAtDesc(vehicleId);
        log.info("getTripsByVehicleId(): found {} trips for vehicleId={}", trips.size(), vehicleId);
        return trips.stream().map(TripMapper::toResponse).toList();
    }
}
