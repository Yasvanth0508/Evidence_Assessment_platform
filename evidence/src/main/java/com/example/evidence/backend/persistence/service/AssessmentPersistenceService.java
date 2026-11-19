package com.example.evidence.backend.persistence.service;

import com.example.evidence.backend.persistence.entity.*;
import com.example.evidence.backend.persistence.mapper.*;
import com.example.evidence.backend.persistence.repository.*;
import com.example.evidence.backend.repositoryanalysis.model.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssessmentPersistenceService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentWorkspaceRepository workspaceRepository;
    private final RepositoryAnalysisRepository analysisRepository;

    private final RepositoryMetadataMapper metadataMapper;
    private final RepositoryWorkspaceMapper workspaceMapper;
    private final RepositoryAnalysisMapper analysisMapper;

    @Transactional(rollbackFor = Exception.class)
    public UUID persistRepositoryStage(
            RepositoryMetadata metadata,
            RepositoryAnalysisResult analysisResult,
            UUID workspaceId,
            UUID candidateId,
            String difficulty,
            Integer durationMinutes,
            Instant scheduledStartAt
    ) {

        // 1. Create Assessment
        Assessment assessment = metadataMapper.toEntity(
                metadata,
                workspaceId,
                candidateId,
                difficulty,
                durationMinutes,
                scheduledStartAt
        );

        // 2. Persist Assessment
        Assessment savedAssessment =
                assessmentRepository.save(assessment);

        // 3. Create and persist AssessmentWorkspace
        AssessmentWorkspace workspace =
                workspaceMapper.toEntity(savedAssessment, metadata);

        workspaceRepository.save(workspace);

        // 4. Create and persist RepositoryAnalysis
        RepositoryAnalysis analysis =
                analysisMapper.toEntity(savedAssessment, analysisResult);

        analysisRepository.save(analysis);

        // 5. Return generated assessment ID
        return savedAssessment.getId();
    }
}