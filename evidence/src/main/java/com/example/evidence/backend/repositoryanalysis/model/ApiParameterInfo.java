package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record ApiParameterInfo(
        String name,
        String type,
        List<String> annotations,
        boolean required
) {}