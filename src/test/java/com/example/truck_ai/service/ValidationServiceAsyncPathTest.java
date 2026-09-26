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
import com.example.truck_ai.events.TripEventExtractedEvent;
import com.example.truck_ai.repository.TripEventRepository;
import com.example.truck_ai.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// Deliberately NOT @Transactional/@Rollback: this test goes through the *real* production
// path (ApplicationEventPublisher.publishEvent -> @Async @EventListener on a separate
// thread), the same way ExtractionService actually triggers ValidationService. A
// @Transactional test method would mask a transaction-boundary bug, because the test's own
// open transaction/connection would already be bound to the thread the assertions run on —
// it would NOT catch a case where the async listener itself fails to open a transaction
// (e.g. via Spring's self-invocation pitfall: @Transactional on an internally-called method
// is silently skipped by proxy-based AOP unless the externally-invoked entry point is also
// annotated). Data is committed for real here and cleaned up manually at the end.
@SpringBootTest
class ValidationServiceAsyncPathTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private TripService tripService;

    @Autowired
    private TripEventRepository tripEventRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void realAsyncEventPath_confirmsEventAndAdvancesTrip_withoutLazyInitException() throws InterruptedException {
        VehicleResponse vehicle = vehicleService.createVehicle(new VehicleRequest("Async Test Driver", 999L));
        TripResponse trip = tripService.createTrip(new TripRequest(vehicle.getVehicleId(), "Origin", "Destination", null));
        Long tripId = trip.getTripId();

        TripEvent event = TripEvent.builder()
                .trip(tripRepository.getReferenceById(tripId))
                .rawText("async path test message")
                .sourceChannel(SourceChannel.PLAIN_TEXT)
                .senderName("tester")
                .receivedAt(LocalDateTime.now())
                .eventType(EventType.LOAD)
                .extractedLocation("Somewhere")
                .extractedTimestamp(LocalDateTime.now())
                .detectedLanguage("en")
                .confidence(0.9)
                .status(EventStatus.EXTRACTED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        Long eventId = tripEventRepository.save(event).getEventId();

        try {
            // Exactly mirrors what ExtractionService.extract() does at the end of a real run.
            eventPublisher.publishEvent(new TripEventExtractedEvent(eventId));

            EventStatus finalStatus = null;
            for (int i = 0; i < 25; i++) {
                Thread.sleep(200);
                finalStatus = tripEventRepository.findById(eventId).orElseThrow().getStatus();
                if (finalStatus != EventStatus.EXTRACTED) {
                    break;
                }
            }

            if (finalStatus == EventStatus.EXTRACTED) {
                fail("ValidationService never processed the event within 5s — the async listener " +
                        "either isn't firing or is throwing (check for LazyInitializationException).");
            }
            assertEquals(EventStatus.CONFIRMED, finalStatus);
            assertEquals(TripStatus.IN_TRANSIT, tripRepository.findById(tripId).orElseThrow().getStatus());
        } finally {
            tripEventRepository.deleteById(eventId);
            tripRepository.deleteById(tripId);
            // Vehicle has no FK dependents left; leaving it is harmless test-data noise,
            // consistent with the other services having no delete endpoint either.
        }
    }
}
