package com.example.evidence.backend.assessment.service;

import com.example.evidence.backend.assessment.dto.AssessmentPreparationRequest;
import com.example.evidence.backend.persistence.service.AssessmentPersistenceService;
import com.example.evidence.backend.repositoryanalysis.git.RepositoryCleanupService;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryMetadata;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryAnalysisResult;
import com.example.evidence.backend.repositoryanalysis.service.RepositoryManagerService;
import com.example.evidence.backend.repositoryanalysis.service.RepositoryAnalysisService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssessmentPreparationService {

    private final RepositoryManagerService repositoryManagerService;
    private final RepositoryAnalysisService repositoryAnalysisService;
    private final AssessmentPersistenceService persistenceService;
    private final RepositoryCleanupService cleanupService;

    public UUID prepareAssessment(AssessmentPreparationRequest request) {

        RepositoryMetadata metadata = null;

        try {
            // Step 1: Clone and validate repository
            metadata = repositoryManagerService.analyzeRepository(
                    request.repositoryRequest()
            );

            // Step 2: Analyze repository
            RepositoryAnalysisResult analysisResult =
                    repositoryAnalysisService.analyze(
                            metadata.backendRootPath()
                    );

            // Step 3: Persist repository-stage data
            return persistenceService.persistRepositoryStage(
                    metadata,
                    analysisResult,
                    request.workspaceId(),
                    request.candidateId(),
                    request.difficulty(),
                    request.durationMinutes(),
                    request.scheduledStartAt()
            );

        } catch (RuntimeException exception) {

            // Clean up if analysis or persistence fails
            if (metadata != null) {
                cleanupService.cleanup(
                        metadata.repositoryLocalPath(),
                        exception
                );
            }

            throw exception;
        }
    }
}