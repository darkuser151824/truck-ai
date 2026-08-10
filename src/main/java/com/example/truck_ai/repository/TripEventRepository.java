package com.example.truck_ai.repository;

import com.example.truck_ai.entity.TripEvent;
import com.example.truck_ai.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripEventRepository extends JpaRepository<TripEvent, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    // Ordered by extractedTimestamp, not receivedAt, per the project's timeline-ordering rule.
    @Query("SELECT e FROM TripEvent e WHERE e.trip.tripId = :tripId ORDER BY e.extractedTimestamp ASC")
    List<TripEvent> findEventsByTripId(@Param("tripId") Long tripId);

    // Dashboard/listing ordering: most recent first, falling back to receivedAt only when
    // extractedTimestamp is null. Sort is fixed in the query itself (not derived from the
    // Pageable) so callers can page but not override the sort.
    @Query(value = "SELECT e FROM TripEvent e ORDER BY COALESCE(e.extractedTimestamp, e.receivedAt) DESC",
            countQuery = "SELECT COUNT(e) FROM TripEvent e")
    Page<TripEvent> findAllOrderedByTimestampDesc(Pageable pageable);

    @Query(value = "SELECT e FROM TripEvent e WHERE e.trip.tripId = :tripId ORDER BY COALESCE(e.extractedTimestamp, e.receivedAt) DESC",
            countQuery = "SELECT COUNT(e) FROM TripEvent e WHERE e.trip.tripId = :tripId")
    Page<TripEvent> findByTripIdOrderedByTimestampDesc(@Param("tripId") Long tripId, Pageable pageable);

    @Query(value = "SELECT e FROM TripEvent e WHERE e.eventType = :eventType ORDER BY COALESCE(e.extractedTimestamp, e.receivedAt) DESC",
            countQuery = "SELECT COUNT(e) FROM TripEvent e WHERE e.eventType = :eventType")
    Page<TripEvent> findByEventTypeOrderedByTimestampDesc(@Param("eventType") EventType eventType, Pageable pageable);

    @Query(value = "SELECT e FROM TripEvent e WHERE e.confidence <= :maxConfidence ORDER BY COALESCE(e.extractedTimestamp, e.receivedAt) DESC",
            countQuery = "SELECT COUNT(e) FROM TripEvent e WHERE e.confidence <= :maxConfidence")
    Page<TripEvent> findByConfidenceLessThanEqualOrderedByTimestampDesc(@Param("maxConfidence") Double maxConfidence, Pageable pageable);

    long countByConfidenceLessThan(Double confidence);
}
