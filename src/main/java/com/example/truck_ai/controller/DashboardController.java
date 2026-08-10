package com.example.truck_ai.controller;

import com.example.truck_ai.dto.DashboardSummaryResponse;
import com.example.truck_ai.service.DashboardService;
import com.example.truck_ai.template.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        log.info("HTTP GET /api/dashboard/summary");
        DashboardSummaryResponse summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Dashboard summary fetched successfully", summary));
    }
}
