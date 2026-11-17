package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record BuildConfiguration(
        String buildTool,
        String javaVersion,
        String springBootVersion,
        List<DependencyInfo> dependencies,
        List<String> plugins
) {}