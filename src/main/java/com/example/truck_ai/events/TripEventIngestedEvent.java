package com.example.truck_ai.events;


public class TripEventIngestedEvent {
    private final Long eventId;
    public TripEventIngestedEvent(Long eventId) { this.eventId = eventId; }
    public Long getEventId() { return eventId; }
}