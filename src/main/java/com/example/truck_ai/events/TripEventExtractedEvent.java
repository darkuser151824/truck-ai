package com.example.truck_ai.events;


public class TripEventExtractedEvent {
    private final Long eventId;
    public TripEventExtractedEvent(Long eventId) { this.eventId = eventId; }
    public Long getEventId() { return eventId; }
}
