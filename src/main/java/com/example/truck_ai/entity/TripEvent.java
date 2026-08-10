package com.example.truck_ai.entity;

import com.example.truck_ai.enums.EventStatus;
import com.example.truck_ai.enums.EventType;
import com.example.truck_ai.enums.IncidentSubType;
import com.example.truck_ai.enums.SourceChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Enumerated(EnumType.STRING)
    private SourceChannel sourceChannel;

    private String senderName;

    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private IncidentSubType incidentSubType;

    private String extractedLocation;

    private LocalDateTime extractedTimestamp;

    private String detectedLanguage;

    private Double confidence;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private Double estimatedDistanceFromPrevious;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;
}
