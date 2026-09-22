package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.dto.PageResponse;
import com.tuckersoft.branchengine.dto.RealityLogResponse;
import com.tuckersoft.branchengine.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
public class DecisionController {

    private final DecisionService service;

    public DecisionController(DecisionService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<DecisionResponse> create(
            @Valid @RequestBody DecisionRequest req,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulate) {
        // SIEMPRE 201, tambien en Modo QA. Un valor desconocido nunca da 400.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, simulate));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DecisionResponse>> list(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.list(branchType, impactLevel, status, playthroughId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogResponse>> realityLogs(@PathVariable Long id) {
        return ResponseEntity.ok(service.realityLogs(id));
    }
}
