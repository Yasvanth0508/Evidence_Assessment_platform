
package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record RepositoryAnalysisResult(
        ProjectStructure projectStructure,
        BuildConfiguration buildConfiguration,
        List<JavaClassInfo> javaClasses,
        List<ApiEndpointInfo> apiEndpoints
) {
    public RepositoryAnalysisResult {
        javaClasses = javaClasses == null
                ? List.of()
                : List.copyOf(javaClasses);

        apiEndpoints = apiEndpoints == null
                ? List.of()
                : List.copyOf(apiEndpoints);
    }
}
