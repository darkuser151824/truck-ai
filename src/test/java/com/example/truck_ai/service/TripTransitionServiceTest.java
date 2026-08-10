package com.example.truck_ai.service;

import com.example.truck_ai.dto.TripRequest;
import com.example.truck_ai.dto.TripResponse;
import com.example.truck_ai.dto.VehicleRequest;
import com.example.truck_ai.dto.VehicleResponse;
import com.example.truck_ai.enums.TripStatus;
import com.example.truck_ai.exception.InvalidTripTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class TripTransitionServiceTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private TripService tripService;

    @Autowired
    private TripTransitionService tripTransitionService;

    private Long createTripForNewVehicle() {
        VehicleResponse vehicle = vehicleService.createVehicle(new VehicleRequest("Test Driver", 999L));
        TripResponse trip = tripService.createTrip(new TripRequest(vehicle.getVehicleId(), "Origin", "Destination", null));
        return trip.getTripId();
    }

    @Test
    void forwardTransitionsSucceedSequentially() {
        Long tripId = createTripForNewVehicle();
        assertEquals(TripStatus.LOADING, tripTransitionService.moveToLoading(tripId).getStatus());
        assertEquals(TripStatus.IN_TRANSIT, tripTransitionService.moveToInTransit(tripId).getStatus());
        assertEquals(TripStatus.UNLOADING, tripTransitionService.moveToUnloading(tripId).getStatus());
        assertEquals(TripStatus.COMPLETED, tripTransitionService.completeTrip(tripId).getStatus());
    }

    @Test
    void skippingIntermediateStatesIsAllowed() {
        Long tripId = createTripForNewVehicle();
        // CREATED -> COMPLETED directly, skipping LOADING/IN_TRANSIT/UNLOADING entirely.
        assertEquals(TripStatus.COMPLETED, tripTransitionService.completeTrip(tripId).getStatus());
    }

    @Test
    void backwardTransitionIsRejected() {
        Long tripId = createTripForNewVehicle();
        tripTransitionService.moveToInTransit(tripId); // CREATED -> IN_TRANSIT (skip allowed)
        assertThrows(InvalidTripTransitionException.class, () -> tripTransitionService.moveToLoading(tripId));
    }

    @Test
    void sidewaysTransitionToSameStateIsRejected() {
        Long tripId = createTripForNewVehicle();
        tripTransitionService.moveToLoading(tripId);
        assertThrows(InvalidTripTransitionException.class, () -> tripTransitionService.moveToLoading(tripId));
    }

    @Test
    void cancelIsAllowedFromAnyNonTerminalState() {
        Long tripId = createTripForNewVehicle();
        tripTransitionService.moveToLoading(tripId);
        assertEquals(TripStatus.CANCELLED, tripTransitionService.cancelTrip(tripId).getStatus());
    }

    @Test
    void noTransitionsAllowedAfterCompleted() {
        Long tripId = createTripForNewVehicle();
        tripTransitionService.completeTrip(tripId);
        assertThrows(InvalidTripTransitionException.class, () -> tripTransitionService.cancelTrip(tripId));
        assertThrows(InvalidTripTransitionException.class, () -> tripTransitionService.moveToLoading(tripId));
    }

    @Test
    void noTransitionsAllowedAfterCancelled() {
        Long tripId = createTripForNewVehicle();
        tripTransitionService.cancelTrip(tripId);
        assertThrows(InvalidTripTransitionException.class, () -> tripTransitionService.completeTrip(tripId));
        assertThrows(InvalidTripTransitionException.class, () -> tripTransitionService.cancelTrip(tripId));
    }
}
