package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record JavaMethodInfo(
        String name,
        String returnType,
        List<JavaParameterInfo> parameters,
        List<JavaAnnotationInfo> annotations,
        String accessModifier,
        boolean isStatic
) {
    public JavaMethodInfo {
        parameters = parameters == null
                ? List.of()
                : List.copyOf(parameters);

        annotations = annotations == null
                ? List.of()
                : List.copyOf(annotations);
    }
}