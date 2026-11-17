package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.model.ApiEndpointInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaClassInfo;

import java.util.List;

public interface ApiEndpointAnalyzer {

    List<ApiEndpointInfo> analyze(List<JavaClassInfo> javaClasses);
}