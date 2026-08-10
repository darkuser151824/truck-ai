package com.example.truck_ai.dto;

public record ExtractedEvent(
        String eventType,          // LOAD, UNLOAD, LOCATION_UPDATE, INCIDENT, LOCATION_QUERY
        String incidentSubType,    // nullable — only when eventType = INCIDENT
        String extractedLocation,  // nullable
        String extractedTimestamp, // nullable — ISO-8601
        String detectedLanguage,   // "hi", "en", or "mixed"
        double confidence          // 0.0 - 1.0
) {}