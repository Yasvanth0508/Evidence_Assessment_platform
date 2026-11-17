package com.example.evidence.backend.repositoryanalysis.model;

public record DependencyInfo(
        String groupId,
        String artifactId,
        String version,
        String scope
) {}