package com.example.truck_ai.dto;

import com.example.truck_ai.enums.SourceChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionRequest {

    @NotNull(message = "tripId must not be null")
    private Long tripId;

    // Not validated here: the controller overwrites this with the fixed
    // channel for the endpoint (PLAIN_TEXT/FORM) after binding, and the
    // whatsapp path builds these programmatically and never runs @Valid.
    private SourceChannel sourceChannel;

    @NotBlank(message = "senderName must not be blank")
    private String senderName;

    @NotBlank(message = "rawText must not be blank")
    private String rawText;

    private LocalDateTime receivedAt;
}
