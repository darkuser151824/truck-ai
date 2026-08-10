package com.example.truck_ai.entity;

import com.example.truck_ai.enums.TripStatus;
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

// NOTE: "only one active Trip per Vehicle" is enforced by a partial unique
// index (uq_vehicle_active_trip) applied directly on the DB, since this
// isn't expressible via standard JPA/Hibernate annotations and there's no
// migration tool (Flyway/Liquibase) wired up yet.
@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private String originPlace;

    private String destinationPlace;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
