package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.entity.RealityLog;

import java.time.Instant;

public record RealityLogResponse(Long id, Long decisionId, String recipientEmail, String subject,
                                 String logStatus, String errorMessage, Instant sentAt, Instant createdAt) {
    public static RealityLogResponse from(RealityLog r) {
        return new RealityLogResponse(r.getId(), r.getDecision().getId(), r.getRecipientEmail(),
                r.getSubject(), r.getLogStatus(), r.getErrorMessage(), r.getSentAt(), r.getCreatedAt());
    }
}
