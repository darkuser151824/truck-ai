package com.example.truck_ai.entity;

import com.example.truck_ai.enums.ComplianceState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

// One row per active Trip; row is removed/archived once the trip completes.
@Entity
@Table(name = "compliance_status")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceStatus {

    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    private LocalDateTime lastConfirmedCheckInAt;

    @Enumerated(EnumType.STRING)
    private ComplianceState complianceState;

    private LocalDateTime flaggedAt;

    private boolean adminNotified;
}
