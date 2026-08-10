package com.example.truck_ai.repository;

import com.example.truck_ai.entity.Trip;
import com.example.truck_ai.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("SELECT t FROM Trip t WHERE t.vehicle.vehicleId = :vehicleId AND t.status NOT IN :excludedStatuses")
    Optional<Trip> findActiveTripByVehicleId(@Param("vehicleId") Long vehicleId, @Param("excludedStatuses") List<TripStatus> excludedStatuses);

    List<Trip> findByStatus(TripStatus status);

    List<Trip> findByVehicle_VehicleIdOrderByStartedAtDesc(Long vehicleId);

    @Query("SELECT t.status AS status, COUNT(t) AS count FROM Trip t GROUP BY t.status")
    List<TripStatusCount> countTripsByStatus();

    interface TripStatusCount {
        TripStatus getStatus();
        Long getCount();
    }
}
