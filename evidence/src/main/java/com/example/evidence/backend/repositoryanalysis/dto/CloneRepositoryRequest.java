package com.example.evidence.backend.repositoryanalysis.dto;

import jakarta.validation.constraints.NotBlank;

public record CloneRepositoryRequest(

        @NotBlank(message = "Repository URL is required")
        String repositoryUrl,

        @NotBlank(message = "Branch name is required")
        String branch,

        @NotBlank(message = "Backend root directory is required")
        String backendRootDirectory

) {
}