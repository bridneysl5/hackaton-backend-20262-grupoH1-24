package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.entity.User;

import java.time.Instant;

// NUNCA incluye password.
public record UserResponse(Long id, String email, String displayName, String role, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getCreatedAt());
    }
}
