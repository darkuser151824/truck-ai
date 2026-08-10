package com.example.truck_ai.enums;

public enum TripStatus {
    CREATED(1),
    LOADING(2),
    IN_TRANSIT(3),
    UNLOADING(4),
    COMPLETED(5),
    CANCELLED(-1);

    private final int rank;

    TripStatus(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
