package com.skillmatrix.requirements.dto;

public record RequirementStatusRequest(
        boolean active,
        String reason
) {}
