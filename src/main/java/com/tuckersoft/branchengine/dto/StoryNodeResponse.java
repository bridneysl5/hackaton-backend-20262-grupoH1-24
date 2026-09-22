package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.entity.StoryNode;

import java.time.Instant;

public record StoryNodeResponse(Long id, String nodeCode, String title, String sceneText,
                                Integer branchCapacity, Integer currentBranches,
                                String primaryBranchCode, String glitchBranchCode, Instant createdAt) {
    public static StoryNodeResponse from(StoryNode n) {
        return new StoryNodeResponse(n.getId(), n.getNodeCode(), n.getTitle(), n.getSceneText(),
                n.getBranchCapacity(), n.getCurrentBranches(),
                n.getPrimaryBranchCode(), n.getGlitchBranchCode(), n.getCreatedAt());
    }
}
