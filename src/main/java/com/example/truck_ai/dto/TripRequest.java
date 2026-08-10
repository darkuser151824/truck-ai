package com.example.truck_ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripRequest {

    @NotNull(message = "vehicleId must not be null")
    private Long vehicleId;

    @NotBlank(message = "originPlace must not be blank")
    private String originPlace;

    @NotBlank(message = "destinationPlace must not be blank")
    private String destinationPlace;

    private LocalDateTime startedAt;
}
