package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.*;

public record StoryNodeRequest(
        @NotBlank @Size(min = 3, max = 40) String nodeCode,
        @NotBlank @Size(min = 3, max = 80) String title,
        @NotBlank @Size(min = 10) String sceneText,
        @NotNull @Positive Integer branchCapacity,   // > 0 : branchCapacity=0 -> 400
        String primaryBranchCode,
        String glitchBranchCode
) {}
