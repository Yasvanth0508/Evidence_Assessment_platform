
package com.example.evidence.backend.persistence.service;

import com.example.evidence.backend.persistence.entity.Assessment;
import com.example.evidence.backend.persistence.entity.RepositoryAnalysis;
import com.example.evidence.backend.persistence.mapper.RepositoryAnalysisMapper;
import com.example.evidence.backend.persistence.repository.AssessmentRepository;
import com.example.evidence.backend.persistence.repository.RepositoryAnalysisRepository;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryAnalysisResult;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepositoryAnalysisPersistenceService {

    private final AssessmentRepository assessmentRepository;
    private final RepositoryAnalysisRepository analysisRepository;
    private final RepositoryAnalysisMapper analysisMapper;

    @Transactional(rollbackFor = Exception.class)
    public void persistAnalysis(
            UUID assessmentId,
            RepositoryAnalysisResult result
    ) {

        // 1. Retrieve the existing assessment
        Assessment assessment = assessmentRepository
                .findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assessment not found: " + assessmentId
                ));

        // 2. Convert analysis result into entity
        RepositoryAnalysis analysis =
                analysisMapper.toEntity(assessment, result);

        // 3. Save analysis
        analysisRepository.save(analysis);
    }
}
