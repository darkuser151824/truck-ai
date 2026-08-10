package com.example.truck_ai.service;

import com.example.truck_ai.dto.PageResponse;
import com.example.truck_ai.dto.TripEventResponse;
import com.example.truck_ai.entity.TripEvent;
import com.example.truck_ai.enums.EventType;
import com.example.truck_ai.mapper.TripEventMapper;
import com.example.truck_ai.repository.TripEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripEventService {

    private final TripEventRepository tripEventRepository;

    public TripEventResponse getEventById(Long eventId) {
        log.debug("getEventById() start: eventId={}", eventId);
        TripEvent event = tripEventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("getEventById() rejected: no TripEvent found for eventId={}", eventId);
                    return new EntityNotFoundException("No trip event found for eventId=" + eventId);
                });
        return TripEventMapper.toResponse(event);
    }

    public PageResponse<TripEventResponse> getAllEvents(Pageable pageable) {
        log.debug("getAllEvents() start: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        // Sorted by extractedTimestamp desc, falling back to receivedAt desc when null,
        // per the project's event-listing sort rule.
        Page<TripEvent> events = tripEventRepository.findAllOrderedByTimestampDesc(pageable);
        log.info("getAllEvents(): found {} events (page {} of {})", events.getNumberOfElements(), events.getNumber(), events.getTotalPages());
        return TripEventMapper.toPageResponse(events);
    }

    public List<TripEventResponse> getEventsByTripId(Long tripId) {
        log.debug("getEventsByTripId() start: tripId={}", tripId);
        List<TripEvent> events = tripEventRepository.findEventsByTripId(tripId);
        log.info("getEventsByTripId(): found {} events for tripId={}", events.size(), tripId);
        return events.stream().map(TripEventMapper::toResponse).toList();
    }

    public PageResponse<TripEventResponse> getEventsByTripIdSortedDesc(Long tripId, Pageable pageable) {
        log.debug("getEventsByTripIdSortedDesc() start: tripId={}, page={}, size={}", tripId, pageable.getPageNumber(), pageable.getPageSize());
        Page<TripEvent> events = tripEventRepository.findByTripIdOrderedByTimestampDesc(tripId, pageable);
        log.info("getEventsByTripIdSortedDesc(): found {} events for tripId={} (page {} of {})", events.getNumberOfElements(), tripId, events.getNumber(), events.getTotalPages());
        return TripEventMapper.toPageResponse(events);
    }

    public PageResponse<TripEventResponse> getEventsByType(EventType eventType, Pageable pageable) {
        log.debug("getEventsByType() start: eventType={}, page={}, size={}", eventType, pageable.getPageNumber(), pageable.getPageSize());
        Page<TripEvent> events = tripEventRepository.findByEventTypeOrderedByTimestampDesc(eventType, pageable);
        log.info("getEventsByType(): found {} events for eventType={} (page {} of {})", events.getNumberOfElements(), eventType, events.getNumber(), events.getTotalPages());
        return TripEventMapper.toPageResponse(events);
    }

    public PageResponse<TripEventResponse> getEventsByMaxConfidence(Double maxConfidence, Pageable pageable) {
        log.debug("getEventsByMaxConfidence() start: maxConfidence={}, page={}, size={}", maxConfidence, pageable.getPageNumber(), pageable.getPageSize());
        Page<TripEvent> events = tripEventRepository.findByConfidenceLessThanEqualOrderedByTimestampDesc(maxConfidence, pageable);
        log.info("getEventsByMaxConfidence(): found {} events with confidence<={} (page {} of {})", events.getNumberOfElements(), maxConfidence, events.getNumber(), events.getTotalPages());
        return TripEventMapper.toPageResponse(events);
    }
}
