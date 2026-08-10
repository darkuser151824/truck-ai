package com.example.truck_ai.dto;

import java.util.List;

public record IngestionBatchResult(List<TripEventResponse> ingested, int duplicatesSkipped) {
}
