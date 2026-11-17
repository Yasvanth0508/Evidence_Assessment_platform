package com.example.evidence.backend.repositoryanalysis.service;

import com.example.evidence.backend.repositoryanalysis.model.RepositoryAnalysisResult;

import java.nio.file.Path;

public interface RepositoryAnalysisService {

    RepositoryAnalysisResult analyze(Path backendRootPath);
}