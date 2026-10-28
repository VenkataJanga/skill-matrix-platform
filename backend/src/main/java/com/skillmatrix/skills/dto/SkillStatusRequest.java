package com.skillmatrix.skills.dto;

public record SkillStatusRequest(
        boolean active,
        String reason
) {}
