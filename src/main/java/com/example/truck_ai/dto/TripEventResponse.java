package com.example.truck_ai.dto;

import com.example.truck_ai.enums.EventStatus;
import com.example.truck_ai.enums.EventType;
import com.example.truck_ai.enums.IncidentSubType;
import com.example.truck_ai.enums.SourceChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripEventResponse {

    private Long eventId;
    private Long tripId;
    private String rawText;
    private SourceChannel sourceChannel;
    private String senderName;
    private LocalDateTime receivedAt;
    private EventType eventType;
    private IncidentSubType incidentSubType;
    private String extractedLocation;
    private LocalDateTime extractedTimestamp;
    private String detectedLanguage;
    private Double confidence;
    private EventStatus status;
    private Double estimatedDistanceFromPrevious;
    private String idempotencyKey;
}
