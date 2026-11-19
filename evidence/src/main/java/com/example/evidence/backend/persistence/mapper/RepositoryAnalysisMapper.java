
package com.example.evidence.backend.persistence.mapper;

import com.example.evidence.backend.persistence.entity.Assessment;
import com.example.evidence.backend.persistence.entity.RepositoryAnalysis;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryAnalysisResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class RepositoryAnalysisMapper {

    private final ObjectMapper objectMapper;

    public RepositoryAnalysisMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RepositoryAnalysis toEntity(
            Assessment assessment,
            RepositoryAnalysisResult result
    ) {

        RepositoryAnalysis analysis = new RepositoryAnalysis();

        analysis.setAssessment(assessment);

        analysis.setProjectStructure(
                convertToJson(result.projectStructure())
        );

        analysis.setSourceCodeStructure(
                convertToJson(result.javaClasses())
        );

        Map<String, Object> content = Map.of(
                "buildConfiguration", result.buildConfiguration(),
                "apiEndpoints", result.apiEndpoints()
        );

        analysis.setContentDetails(convertToJson(content));

        analysis.setCompletedAt(Instant.now());

        return analysis;
    }

    private JsonNode convertToJson(Object value) {

        try {
            return objectMapper.valueToTree(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Failed to convert repository analysis into JSON",
                    exception
            );
        }
    }
}
