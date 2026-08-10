package com.example.truck_ai.controller;

import com.example.truck_ai.dto.IngestionBatchResult;
import com.example.truck_ai.dto.IngestionRequest;
import com.example.truck_ai.dto.TripEventResponse;
import com.example.truck_ai.dto.WhatsAppDumpRequest;
import com.example.truck_ai.enums.SourceChannel;
import com.example.truck_ai.service.IngestionService;
import com.example.truck_ai.template.ApiResponse;
import com.example.truck_ai.util.WhatsAppExportParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/plain-text")
    public ResponseEntity<ApiResponse<TripEventResponse>> ingestPlainText(@Valid @RequestBody IngestionRequest request) {
        log.info("HTTP POST /api/ingestion/plain-text: tripId={}, sender={}", request.getTripId(), request.getSenderName());
        request.setSourceChannel(SourceChannel.PLAIN_TEXT);
        TripEventResponse saved = ingestionService.ingest(request);
        log.info("HTTP POST /api/ingestion/plain-text: 201 eventId={}", saved.getEventId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Trip event ingested successfully", saved));
    }

    @PostMapping("/form")
    public ResponseEntity<ApiResponse<TripEventResponse>> ingestForm(@Valid @RequestBody IngestionRequest request) {
        log.info("HTTP POST /api/ingestion/form: tripId={}, sender={}", request.getTripId(), request.getSenderName());
        request.setSourceChannel(SourceChannel.FORM);
        TripEventResponse saved = ingestionService.ingest(request);
        log.info("HTTP POST /api/ingestion/form: 201 eventId={}", saved.getEventId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Trip event ingested successfully", saved));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<ApiResponse<IngestionBatchResult>> ingestWhatsAppDump(@Valid @RequestBody WhatsAppDumpRequest dumpRequest) {
        log.info("HTTP POST /api/ingestion/whatsapp: tripId={}, dumpLength={} chars",
                dumpRequest.getTripId(), dumpRequest.getRawDump() == null ? 0 : dumpRequest.getRawDump().length());

        List<IngestionRequest> parsed = WhatsAppExportParser.parse(dumpRequest.getTripId(), dumpRequest.getRawDump());
        log.info("HTTP POST /api/ingestion/whatsapp: parser produced {} messages", parsed.size());

        IngestionBatchResult result = ingestionService.ingestBatch(parsed);
        log.info("HTTP POST /api/ingestion/whatsapp: 201, ingested={}, duplicatesSkipped={}",
                result.ingested().size(), result.duplicatesSkipped());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "WhatsApp dump ingested successfully", result));
    }
}
