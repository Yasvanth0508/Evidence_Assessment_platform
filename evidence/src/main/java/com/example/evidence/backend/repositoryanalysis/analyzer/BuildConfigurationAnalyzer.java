package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.model.BuildConfiguration;

import java.nio.file.Path;

public interface BuildConfigurationAnalyzer {

    BuildConfiguration analyze(Path backendRootPath);

}