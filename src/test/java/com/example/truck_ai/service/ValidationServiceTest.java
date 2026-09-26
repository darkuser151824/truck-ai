package com.example.truck_ai.service;

import com.example.truck_ai.dto.TripRequest;
import com.example.truck_ai.dto.TripResponse;
import com.example.truck_ai.dto.VehicleRequest;
import com.example.truck_ai.dto.VehicleResponse;
import com.example.truck_ai.entity.TripEvent;
import com.example.truck_ai.enums.EventStatus;
import com.example.truck_ai.enums.EventType;
import com.example.truck_ai.enums.SourceChannel;
import com.example.truck_ai.enums.TripStatus;
import com.example.truck_ai.repository.TripEventRepository;
import com.example.truck_ai.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Exercises ValidationService (Gate 1 confidence, Gate 2 trip-ordering, routing, and the
// post-CONFIRMED trip-lifecycle advance) directly against EXTRACTED-status events, so it
// doesn't depend on a live LLM call the way the RAW->EXTRACTED step does.
@SpringBootTest
@Transactional
@Rollback
class ValidationServiceTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private TripService tripService;

    @Autowired
    private TripEventRepository tripEventRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ValidationService validationService;

    private Long createTripForNewVehicle() {
        VehicleResponse vehicle = vehicleService.createVehicle(new VehicleRequest("Test Driver", 999L));
        TripResponse trip = tripService.createTrip(new TripRequest(vehicle.getVehicleId(), "Origin", "Destination", null));
        return trip.getTripId();
    }

    private Long extractedEvent(Long tripId, EventType eventType, Double confidence) {
        TripEvent event = TripEvent.builder()
                .trip(tripRepository.getReferenceById(tripId))
                .rawText("test message")
                .sourceChannel(SourceChannel.PLAIN_TEXT)
                .senderName("tester")
                .receivedAt(LocalDateTime.now())
                .eventType(eventType)
                .extractedLocation("Somewhere")
                .extractedTimestamp(LocalDateTime.now())
                .detectedLanguage("en")
                .confidence(confidence)
                .status(EventStatus.EXTRACTED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        return tripEventRepository.save(event).getEventId();
    }

    @Test
    void bothGatesPass_confirmsAndAdvancesTripToInTransit() {
        Long tripId = createTripForNewVehicle();
        Long eventId = extractedEvent(tripId, EventType.LOAD, 0.9);

        validationService.validateById(eventId);

        assertEquals(EventStatus.CONFIRMED, tripEventRepository.findById(eventId).orElseThrow().getStatus());
        assertEquals(TripStatus.IN_TRANSIT, tripRepository.findById(tripId).orElseThrow().getStatus());
    }

    @Test
    void lowConfidence_goesToPendingReview_tripUnchanged() {
        Long tripId = createTripForNewVehicle();
        Long eventId = extractedEvent(tripId, EventType.LOAD, 0.2);

        validationService.validateById(eventId);

        assertEquals(EventStatus.PENDING_REVIEW, tripEventRepository.findById(eventId).orElseThrow().getStatus());
        assertEquals(TripStatus.CREATED, tripRepository.findById(tripId).orElseThrow().getStatus());
    }

    @Test
    void unloadWithoutPriorConfirmedLoad_goesToPendingReview() {
        Long tripId = createTripForNewVehicle();
        Long eventId = extractedEvent(tripId, EventType.UNLOAD, 0.9);

        validationService.validateById(eventId);

        assertEquals(EventStatus.PENDING_REVIEW, tripEventRepository.findById(eventId).orElseThrow().getStatus());
        assertEquals(TripStatus.CREATED, tripRepository.findById(tripId).orElseThrow().getStatus());
    }

    @Test
    void unloadAfterConfirmedLoad_confirmsAndCompletesTrip() {
        Long tripId = createTripForNewVehicle();
        Long loadEventId = extractedEvent(tripId, EventType.LOAD, 0.9);
        validationService.validateById(loadEventId);

        Long unloadEventId = extractedEvent(tripId, EventType.UNLOAD, 0.9);
        validationService.validateById(unloadEventId);

        assertEquals(EventStatus.CONFIRMED, tripEventRepository.findById(unloadEventId).orElseThrow().getStatus());
        assertEquals(TripStatus.COMPLETED, tripRepository.findById(tripId).orElseThrow().getStatus());
    }

    @Test
    void eventAfterTripCompleted_goesToPendingReview_tripUnchanged() {
        Long tripId = createTripForNewVehicle();
        Long loadEventId = extractedEvent(tripId, EventType.LOAD, 0.9);
        validationService.validateById(loadEventId);
        Long unloadEventId = extractedEvent(tripId, EventType.UNLOAD, 0.9);
        validationService.validateById(unloadEventId);
        // trip is now COMPLETED (terminal)

        Long secondLoadEventId = extractedEvent(tripId, EventType.LOAD, 0.9);
        validationService.validateById(secondLoadEventId);

        assertEquals(EventStatus.PENDING_REVIEW, tripEventRepository.findById(secondLoadEventId).orElseThrow().getStatus());
        assertEquals(TripStatus.COMPLETED, tripRepository.findById(tripId).orElseThrow().getStatus());
    }

    @Test
    void implausibleTimestamp_doesNotBlockConfirmation() {
        Long tripId = createTripForNewVehicle();
        TripEvent event = TripEvent.builder()
                .trip(tripRepository.getReferenceById(tripId))
                .rawText("test message")
                .sourceChannel(SourceChannel.PLAIN_TEXT)
                .senderName("tester")
                .receivedAt(LocalDateTime.now())
                .eventType(EventType.LOAD)
                .extractedLocation("Somewhere")
                // Implausible: a year before the trip even started. Should only be logged,
                // never block CONFIRMED, per the spec's "not a gate" rule.
                .extractedTimestamp(LocalDateTime.now().minusYears(1))
                .detectedLanguage("en")
                .confidence(0.9)
                .status(EventStatus.EXTRACTED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        Long eventId = tripEventRepository.save(event).getEventId();

        validationService.validateById(eventId);

        assertEquals(EventStatus.CONFIRMED, tripEventRepository.findById(eventId).orElseThrow().getStatus());
        assertEquals(TripStatus.IN_TRANSIT, tripRepository.findById(tripId).orElseThrow().getStatus());
    }
}
