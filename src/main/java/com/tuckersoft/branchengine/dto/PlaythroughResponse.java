package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.entity.Playthrough;

import java.time.Instant;

public record PlaythroughResponse(Long id, String playerTag, String ownerEmail,
                                  String startNodeCode, String currentNodeCode,
                                  Integer lucidity, Integer controlLevel, String status,
                                  String endingCode, Instant createdAt, Instant updatedAt) {
    public static PlaythroughResponse from(Playthrough p) {
        return new PlaythroughResponse(
                p.getId(), p.getPlayerTag(), p.getUser().getEmail(),
                p.getStartNodeCode(),
                p.getCurrentNode() == null ? null : p.getCurrentNode().getNodeCode(),
                p.getLucidity(), p.getControlLevel(), p.getStatus(),
                p.getEndingCode(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
