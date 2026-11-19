package com.example.evidence.backend.persistence.repository;

import com.example.evidence.backend.persistence.entity.RepositoryAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RepositoryAnalysisRepository
        extends JpaRepository<RepositoryAnalysis, UUID> {
}