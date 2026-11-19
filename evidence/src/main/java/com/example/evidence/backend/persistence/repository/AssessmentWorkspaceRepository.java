package com.example.evidence.backend.persistence.repository;

import com.example.evidence.backend.persistence.entity.AssessmentWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssessmentWorkspaceRepository
        extends JpaRepository<AssessmentWorkspace, UUID> {
}