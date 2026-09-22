package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.StoryNodeRequest;
import com.tuckersoft.branchengine.dto.StoryNodeResponse;
import com.tuckersoft.branchengine.service.StoryNodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
public class StoryNodeController {

    private final StoryNodeService service;

    public StoryNodeController(StoryNodeService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<StoryNodeResponse> create(@Valid @RequestBody StoryNodeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<StoryNodeResponse>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryNodeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
