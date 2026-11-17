package com.example.evidence.backend.repositoryanalysis.model;

import java.util.Map;

public record JavaAnnotationInfo(
        String name,
        Map<String, String> attributes
) {
    public JavaAnnotationInfo {
        attributes = attributes == null
                ? Map.of()
                : Map.copyOf(attributes);
    }
}