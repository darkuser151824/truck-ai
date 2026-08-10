package com.example.truck_ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequest {

    @NotBlank(message = "currentDriverName must not be blank")
    private String currentDriverName;

    @NotNull(message = "fleetOwnerId must not be null")
    private Long fleetOwnerId;
}
