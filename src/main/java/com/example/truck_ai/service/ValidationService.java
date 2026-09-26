package com.example.truck_ai.service;

import com.example.truck_ai.entity.Trip;
import com.example.truck_ai.entity.TripEvent;
import com.example.truck_ai.enums.EventStatus;
import com.example.truck_ai.enums.EventType;
import com.example.truck_ai.enums.TripStatus;
import com.example.truck_ai.events.TripEventExtractedEvent;
import com.example.truck_ai.exception.InvalidTripTransitionException;
import com.example.truck_ai.repository.TripEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    // Starting value, per spec — tune later.
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    private final TripEventRepository tripEventRepository;
    private final TripTransitionService tripTransitionService;

    // @Transactional here too (not just on validateById): this method is the one Spring's
    // event multicaster actually invokes through the proxy. Since it calls validateById via
    // plain self-invocation, validateById's own @Transactional would be silently skipped by
    // Spring's proxy-based AOP if this method weren't transactional itself — self-invocation
    // never goes through the proxy, so no transaction would open for the real async path.
    @Async
    @EventListener
    @Transactional
    public void onTripEventExtracted(TripEventExtractedEvent event) {
        validateById(event.getEventId());
    }

    @Transactional
    public void validateById(Long eventId) {
        TripEvent event = tripEventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("No event found for eventId=" + eventId));
        Trip trip = event.getTrip();

        boolean gate1Passed = passesConfidenceGate(event);
        boolean gate2Passed = passesTripOrderingGate(event, trip);
        logTimestampPlausibility(event, trip);

        if (gate1Passed && gate2Passed) {
            event.setStatus(EventStatus.CONFIRMED);
            tripEventRepository.save(event);
            advanceTripLifecycle(event, trip);
            log.info("validateById(): eventId={} CONFIRMED (gate1={}, gate2={})",
                    eventId, gate1Passed, gate2Passed);
        } else {
            event.setStatus(EventStatus.PENDING_REVIEW);
            tripEventRepository.save(event);
            log.warn("validateById(): eventId={} PENDING_REVIEW (gate1={}, gate2={})",
                    eventId, gate1Passed, gate2Passed);
        }
    }

    // Gate 1 — confidence threshold.
    private boolean passesConfidenceGate(TripEvent event) {
        Double confidence = event.getConfidence();
        return confidence != null && confidence >= CONFIDENCE_THRESHOLD;
    }

    // Gate 2 — trip-state ordering. Reuses TripTransitionService.validateTransition rather
    // than duplicating its rank/terminal-state logic (same class of check as the earlier
    // race-condition fix there).
    private boolean passesTripOrderingGate(TripEvent event, Trip trip) {
        EventType eventType = event.getEventType();
        TripStatus current = trip.getStatus();

        if (eventType == EventType.UNLOAD) {
            boolean hasPriorConfirmedLoad = tripEventRepository.existsByTripIdAndEventTypeAndStatus(
                    trip.getTripId(), EventType.LOAD, EventStatus.CONFIRMED);
            if (!hasPriorConfirmedLoad) {
                log.warn("passesTripOrderingGate(): eventId={} rejected — UNLOAD with no prior confirmed LOAD on tripId={}",
                        event.getEventId(), trip.getTripId());
                return false;
            }
            return isForwardTransitionValid(trip.getTripId(), current, TripStatus.COMPLETED);
        }

        if (eventType == EventType.LOAD) {
            return isForwardTransitionValid(trip.getTripId(), current, TripStatus.IN_TRANSIT);
        }

        // LOCATION_UPDATE, INCIDENT, LOCATION_QUERY don't drive the trip lifecycle themselves,
        // but they're still implausible against a trip that's already finished.
        return !tripTransitionService.isTerminal(current);
    }

    private boolean isForwardTransitionValid(Long tripId, TripStatus current, TripStatus target) {
        try {
            tripTransitionService.validateTransition(tripId, current, target);
            return true;
        } catch (InvalidTripTransitionException e) {
            log.warn("passesTripOrderingGate(): tripId={} rejected — {}", tripId, e.getMessage());
            return false;
        }
    }

    // Applies the trip-lifecycle side effect only once an event is actually CONFIRMED —
    // an unvalidated extraction must never move the trip forward on its own.
    private void advanceTripLifecycle(TripEvent event, Trip trip) {
        if (event.getEventType() == EventType.LOAD) {
            tripTransitionService.moveToInTransit(trip.getTripId());
        } else if (event.getEventType() == EventType.UNLOAD) {
            tripTransitionService.completeTrip(trip.getTripId());
        }
    }

    // Not a gate — timestamp plausibility is judged more likely to be human error than
    // AI hallucination, so it's logged only, never blocking.
    private void logTimestampPlausibility(TripEvent event, Trip trip) {
        if (event.getExtractedTimestamp() == null) {
            return;
        }
        if (event.getReceivedAt() != null && event.getExtractedTimestamp().isAfter(event.getReceivedAt())) {
            log.warn("logTimestampPlausibility(): eventId={} extractedTimestamp={} is after receivedAt={}",
                    event.getEventId(), event.getExtractedTimestamp(), event.getReceivedAt());
        }
        if (trip.getStartedAt() != null && event.getExtractedTimestamp().isBefore(trip.getStartedAt())) {
            log.warn("logTimestampPlausibility(): eventId={} extractedTimestamp={} is before trip startedAt={}",
                    event.getEventId(), event.getExtractedTimestamp(), trip.getStartedAt());
        }
    }
}
