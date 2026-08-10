package com.example.truck_ai.mapper;

import com.example.truck_ai.dto.PageResponse;
import com.example.truck_ai.dto.TripEventResponse;
import com.example.truck_ai.entity.TripEvent;
import org.springframework.data.domain.Page;

public final class TripEventMapper {

    private TripEventMapper() {
    }

    public static PageResponse<TripEventResponse> toPageResponse(Page<TripEvent> page) {
        PageResponse<TripEventResponse> response = new PageResponse<>();
        response.setContent(page.getContent().stream().map(TripEventMapper::toResponse).toList());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setPageNumber(page.getNumber());
        return response;
    }

    public static TripEventResponse toResponse(TripEvent event) {
        TripEventResponse response = new TripEventResponse();
        response.setEventId(event.getEventId());
        response.setTripId(event.getTrip() != null ? event.getTrip().getTripId() : null);
        response.setRawText(event.getRawText());
        response.setSourceChannel(event.getSourceChannel());
        response.setSenderName(event.getSenderName());
        response.setReceivedAt(event.getReceivedAt());
        response.setEventType(event.getEventType());
        response.setIncidentSubType(event.getIncidentSubType());
        response.setExtractedLocation(event.getExtractedLocation());
        response.setExtractedTimestamp(event.getExtractedTimestamp());
        response.setDetectedLanguage(event.getDetectedLanguage());
        response.setConfidence(event.getConfidence());
        response.setStatus(event.getStatus());
        response.setEstimatedDistanceFromPrevious(event.getEstimatedDistanceFromPrevious());
        response.setIdempotencyKey(event.getIdempotencyKey());
        return response;
    }
}
