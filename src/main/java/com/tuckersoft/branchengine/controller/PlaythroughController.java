package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.PathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.service.PlaythroughService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
public class PlaythroughController {

    private final PlaythroughService service;

    public PlaythroughController(PlaythroughService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PlaythroughResponse> create(@Valid @RequestBody PlaythroughRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<PlaythroughResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PathResponse> getPath(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPath(id));
    }
}
