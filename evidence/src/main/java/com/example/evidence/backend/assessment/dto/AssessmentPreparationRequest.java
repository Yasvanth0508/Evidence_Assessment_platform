package com.example.evidence.backend.assessment.dto;

import com.example.evidence.backend.repositoryanalysis.dto.CloneRepositoryRequest;

import java.time.Instant;
import java.util.UUID;

public record AssessmentPreparationRequest(
        CloneRepositoryRequest repositoryRequest,
        UUID workspaceId,
        UUID candidateId,
        String difficulty,
        Integer durationMinutes,
        Instant scheduledStartAt
) {}