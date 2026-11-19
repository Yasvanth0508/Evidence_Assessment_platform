package com.example.evidence.backend.persistence.repository;

import com.example.evidence.backend.persistence.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository
        extends JpaRepository<Assessment, UUID> {

    Optional<Assessment> findByWorkspaceIdAndCandidateId(
            UUID workspaceId,
            UUID candidateId
    );

    boolean existsByWorkspaceIdAndCandidateId(
            UUID workspaceId,
            UUID candidateId
    );
}