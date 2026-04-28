package com.de.app.agentic.controller;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.TripPlanResponse;
import com.de.app.agentic.orchestrator.TripOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips")
public class TripPlannerController {

    private final TripOrchestratorService orchestratorService;

    @PostMapping("/plan")
    public ResponseEntity<TripPlanResponse> planTrip(@Valid @RequestBody TripPlanRequest request) {
        return ResponseEntity.ok(orchestratorService.planTrip(request));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}