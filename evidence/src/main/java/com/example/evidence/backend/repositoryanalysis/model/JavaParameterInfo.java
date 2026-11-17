package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record JavaParameterInfo(
        String name,
        String type,
        List<JavaAnnotationInfo> annotations
) {
    public JavaParameterInfo {
        annotations = annotations == null
                ? List.of()
                : List.copyOf(annotations);
    }
}