package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record JavaFieldInfo(
        String name,
        String type,
        List<JavaAnnotationInfo> annotations,
        String accessModifier,
        boolean isStatic,
        boolean isFinal
) {
    public JavaFieldInfo {
        annotations = annotations == null
                ? List.of()
                : List.copyOf(annotations);
    }
}