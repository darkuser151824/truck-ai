package com.example.truck_ai.service;

import com.example.truck_ai.dto.ExtractedEvent;
import com.example.truck_ai.entity.Trip;
import com.example.truck_ai.entity.TripEvent;
import com.example.truck_ai.enums.EventStatus;
import com.example.truck_ai.enums.EventType;
import com.example.truck_ai.enums.IncidentSubType;
import com.example.truck_ai.enums.TripStatus;
import com.example.truck_ai.events.TripEventIngestedEvent;
import com.example.truck_ai.repository.TripEventRepository;
import com.example.truck_ai.repository.TripRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final ChatClient chatClient;
    private final TripEventRepository tripEventRepository;
    private final TripTransitionService tripTransitionService;
    private final TripRepository tripRepository;

    private static final String SYSTEM_PROMPT = """
    You are extracting structured fleet-operation data from a raw driver or dispatcher message.
    The message may be written in Hindi, English, or a mix of both (Hinglish).

    Return exactly these fields:

    - eventType: one of LOAD, UNLOAD, LOCATION_UPDATE, INCIDENT, LOCATION_QUERY
      - LOAD: cargo was picked up
      - UNLOAD: cargo was delivered
      - LOCATION_UPDATE: a routine "still fine, here's where I am" report
      - INCIDENT: anything unusual — breakdown, accident, refused delivery, driver change,
        security issue, checkpoint/document problem
      - LOCATION_QUERY: someone is ASKING where the vehicle is, not the vehicle reporting

    - incidentSubType: ONLY set this if eventType is INCIDENT. One of:
      BREAKDOWN, ACCIDENT, DELIVERY_REFUSED, DRIVER_CHANGE, THEFT_OR_SECURITY,
      DOCUMENT_OR_CHECKPOINT_ISSUE, OTHER
      If eventType is not INCIDENT, this must be null.

    - extractedLocation: the place name mentioned in the message, or null if no place is mentioned.

    - extractedTimestamp: resolve any time expression in the message (including relative
      expressions like "abhi", "kal subah", "2 ghante pehle") against the reference time
      provided with the message, and return it in ISO-8601 format. If no time can be
      reasonably determined, return null.

    - detectedLanguage: "hi", "en", or "mixed"

    - confidence: your genuine confidence, from 0.0 to 1.0, that this extraction is correct.
      This must reflect real uncertainty, not just how well-formatted your answer looks.

    CRITICAL RULE: if any field cannot be determined from the message with real confidence,
    return null for that field. Do NOT guess, infer, or default to a plausible-sounding value.
    Returning null for a genuinely unclear field is correct behavior, not a failure.

    Examples:

    Message: "truck 4 abhi Sonepat depot pahuncha, unloading start ho gayi"
    Reference time: 2026-07-21T14:00:00
    Output: {"eventType":"UNLOAD","incidentSubType":null,"extractedLocation":"Sonepat depot",
    "extractedTimestamp":"2026-07-21T14:00:00","detectedLanguage":"mixed","confidence":0.93}

    Message: "Truck broke down near Dhule highway, stuck for 2 hours, mechanic called"
    Reference time: 2023-07-12T18:45:00
    Output: {"eventType":"INCIDENT","incidentSubType":"BREAKDOWN","extractedLocation":"Dhule highway",
    "extractedTimestamp":"2023-07-12T18:45:00","detectedLanguage":"en","confidence":0.91}

    Message: "kahan hai gaadi abhi?"
    Reference time: 2026-07-21T09:00:00
    Output: {"eventType":"LOCATION_QUERY","incidentSubType":null,"extractedLocation":null,
    "extractedTimestamp":null,"detectedLanguage":"hi","confidence":0.88}
    """;

    @Async
    @EventListener
    @Transactional
    public void onTripEventIngested(TripEventIngestedEvent event) {
        extractById(event.getEventId());
    }


    public void extractById(Long eventId) {
        TripEvent tripEvent = tripEventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("No event found for eventId=" + eventId));
        extract(tripEvent);
    }

    private void extract(TripEvent event) {
        try {
            ExtractedEvent result = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("Message: {rawText}\nReference time: {receivedAt}")
                            .param("rawText", event.getRawText())
                            .param("receivedAt", event.getReceivedAt().toString()))
                    .options(AnthropicChatOptions.builder()
                            .model("claude-haiku-4-5-20251001")
                            .temperature(0.0))   // <-- no .build() here anymore
                    .call()
                    .entity(ExtractedEvent.class);

            event.setEventType(EventType.valueOf(result.eventType()));
            event.setIncidentSubType(result.incidentSubType() == null ? null
                    : IncidentSubType.valueOf(result.incidentSubType()));
            event.setExtractedLocation(result.extractedLocation());
            event.setExtractedTimestamp(result.extractedTimestamp() == null ? null
                    : LocalDateTime.parse(result.extractedTimestamp()));
            event.setDetectedLanguage(result.detectedLanguage());
            event.setConfidence(result.confidence());
            event.setStatus(EventStatus.EXTRACTED);
            tripEventRepository.save(event);

            driveTripStatus(event);
            log.info("extract(): eventId={} extracted, eventType={}, confidence={}",
                    event.getEventId(), event.getEventType(), event.getConfidence());
        } catch (Exception e) {
            log.warn("extract(): failed for eventId={}, staying RAW: {}",
                    event.getEventId(), e.getMessage());
        }
    }
    private void driveTripStatus(TripEvent event) {
        Trip trip = event.getTrip();
        if (event.getEventType() == EventType.UNLOAD) {
            trip.setStatus(TripStatus.COMPLETED);
            tripRepository.save(trip);
        } else if (event.getEventType() == EventType.LOAD) {
            trip.setStatus(TripStatus.IN_TRANSIT);
            tripRepository.save(trip);
        }
    }



}
