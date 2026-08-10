package com.example.truck_ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppDumpRequest {

    @NotNull(message = "tripId must not be null")
    private Long tripId;

    @NotBlank(message = "rawDump must not be blank")
    private String rawDump;
}
