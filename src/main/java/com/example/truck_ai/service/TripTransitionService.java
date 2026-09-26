package com.example.truck_ai.service;

import com.example.truck_ai.dto.TripResponse;
import com.example.truck_ai.entity.Trip;
import com.example.truck_ai.enums.TripStatus;
import com.example.truck_ai.exception.InvalidTripTransitionException;
import com.example.truck_ai.mapper.TripMapper;
import com.example.truck_ai.repository.TripRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripTransitionService {

    private final TripRepository tripRepository;

    public TripResponse moveToLoading(Long tripId) {
        return transition(tripId, TripStatus.LOADING);
    }

    public TripResponse moveToInTransit(Long tripId) {
        return transition(tripId, TripStatus.IN_TRANSIT);
    }

    public TripResponse moveToUnloading(Long tripId) {
        return transition(tripId, TripStatus.UNLOADING);
    }

    public TripResponse completeTrip(Long tripId) {
        return transition(tripId, TripStatus.COMPLETED);
    }

    public TripResponse cancelTrip(Long tripId) {
        return transition(tripId, TripStatus.CANCELLED);
    }

    private TripResponse transition(Long tripId, TripStatus target) {
        log.debug("transition() start: tripId={}, target={}", tripId, target);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> {
                    log.warn("transition() rejected: no Trip found for tripId={}", tripId);
                    return new EntityNotFoundException("No trip found for tripId=" + tripId);
                });

        TripStatus current = trip.getStatus();
        validateTransition(tripId, current, target);

        trip.setStatus(target);
        if (target == TripStatus.COMPLETED) {
            trip.setCompletedAt(LocalDateTime.now());
        }

        Trip saved = tripRepository.save(trip);
        log.info("transition(): tripId={} moved {} -> {}", tripId, current, target);
        return TripMapper.toResponse(saved);
    }

    public void validateTransition(Long tripId, TripStatus current, TripStatus target) {
        if (isTerminal(current)) {
            log.warn("validateTransition() rejected: tripId={} is already terminal at {}", tripId, current);
            throw new InvalidTripTransitionException(tripId, current, target);
        }

        if (target == TripStatus.CANCELLED) {
            return;
        }

        if (target.getRank() <= current.getRank()) {
            log.warn("validateTransition() rejected: tripId={} cannot move {} -> {} (not forward)", tripId, current, target);
            throw new InvalidTripTransitionException(tripId, current, target);
        }
    }

    public boolean isTerminal(TripStatus status) {
        return status == TripStatus.COMPLETED || status == TripStatus.CANCELLED;
    }
}
