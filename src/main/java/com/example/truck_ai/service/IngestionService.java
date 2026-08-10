package com.example.truck_ai.service;

import com.example.truck_ai.dto.IngestionBatchResult;
import com.example.truck_ai.dto.IngestionRequest;
import com.example.truck_ai.dto.TripEventResponse;
import com.example.truck_ai.entity.Trip;
import com.example.truck_ai.entity.TripEvent;
import com.example.truck_ai.enums.EventStatus;
import com.example.truck_ai.events.TripEventIngestedEvent;
import com.example.truck_ai.exception.DuplicateTripEventException;
import com.example.truck_ai.mapper.TripEventMapper;
import com.example.truck_ai.repository.TripEventRepository;
import com.example.truck_ai.repository.TripRepository;
import com.example.truck_ai.util.IdempotencyKeyGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final TripRepository tripRepository;
    private final TripEventRepository tripEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TripEventResponse ingest(IngestionRequest request) {
        log.debug("ingest() start: tripId={}, channel={}, sender={}",
                request.getTripId(), request.getSourceChannel(), request.getSenderName());

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> {
                    log.warn("ingest() rejected: no Trip found for tripId={}", request.getTripId());
                    return new EntityNotFoundException("No trip found for tripId=" + request.getTripId());
                });
        log.trace("ingest(): Trip found for tripId={}, status={}", trip.getTripId(), trip.getStatus());

        String idempotencyKey = IdempotencyKeyGenerator.generate(
                request.getTripId(), request.getSourceChannel(), request.getSenderName(), request.getRawText());
        log.debug("ingest(): computed idempotencyKey={}", idempotencyKey);

        // Pre-check first (cheap, avoids a failed insert in the common case);
        // the DB's unique constraint on idempotencyKey is the real guarantee
        // and is caught below to close the race between concurrent duplicate
        // deliveries (e.g. a WhatsApp webhook retry landing at the same time).
        if (tripEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("ingest() rejected: duplicate idempotencyKey={} already exists", idempotencyKey);
            throw new DuplicateTripEventException(idempotencyKey);
        }

        TripEvent event = TripEvent.builder()
                .trip(trip)
                .rawText(request.getRawText())
                .sourceChannel(request.getSourceChannel())
                .senderName(request.getSenderName())
                .receivedAt(request.getReceivedAt() != null ? request.getReceivedAt() : LocalDateTime.now())
                .status(EventStatus.RAW)
                .idempotencyKey(idempotencyKey)
                .build();
        log.trace("ingest(): built TripEvent (pre-save): rawText='{}', receivedAt={}", event.getRawText(), event.getReceivedAt());

        try {
            TripEvent saved = tripEventRepository.save(event);
            log.info("ingest(): saved TripEvent eventId={} for tripId={}, idempotencyKey={}",
                    saved.getEventId(), request.getTripId(), idempotencyKey);
            eventPublisher.publishEvent(new TripEventIngestedEvent(saved.getEventId()));
            return TripEventMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("ingest() rejected: DB unique constraint caught a race-condition duplicate, idempotencyKey={}", idempotencyKey);
            throw new DuplicateTripEventException(idempotencyKey);
        }
    }

    public IngestionBatchResult ingestBatch(List<IngestionRequest> requests) {
        log.info("ingestBatch() start: {} messages to process", requests.size());
        List<TripEventResponse> ingested = new ArrayList<>();
        int duplicatesSkipped = 0;

        for (int i = 0; i < requests.size(); i++) {
            IngestionRequest request = requests.get(i);
            log.debug("ingestBatch(): item {}/{}, sender={}", i + 1, requests.size(), request.getSenderName());
            try {
                ingested.add(ingest(request));
            } catch (DuplicateTripEventException e) {
                log.info("ingestBatch(): item {}/{} skipped as duplicate", i + 1, requests.size());
                duplicatesSkipped++;
            }
        }

        log.info("ingestBatch() done: {} ingested, {} duplicates skipped", ingested.size(), duplicatesSkipped);
        return new IngestionBatchResult(ingested, duplicatesSkipped);
    }
}
