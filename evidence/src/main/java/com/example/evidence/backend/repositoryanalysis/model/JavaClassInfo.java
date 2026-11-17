package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record JavaClassInfo(
        String packageName,
        String className,
        String declarationType,
        List<JavaAnnotationInfo> annotations,
        String accessModifier,
        String parentClass,
        List<String> implementedInterfaces,
        List<JavaFieldInfo> fields,
        List<JavaMethodInfo> methods,
        String sourceFile
) {
    public JavaClassInfo {
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        implementedInterfaces = implementedInterfaces == null
                ? List.of() : List.copyOf(implementedInterfaces);
        fields = fields == null ? List.of() : List.copyOf(fields);
        methods = methods == null ? List.of() : List.copyOf(methods);
    }
}