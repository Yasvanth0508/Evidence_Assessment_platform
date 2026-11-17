
package com.example.evidence.backend.repositoryanalysis.service;

import com.example.evidence.backend.repositoryanalysis.analyzer.DefaultBuildConfigurationAnalyzer;
import com.example.evidence.backend.repositoryanalysis.analyzer.DefaultGradleConfigurationAnalyzer;
import com.example.evidence.backend.repositoryanalysis.analyzer.JavaSourceAnalyzer;
import com.example.evidence.backend.repositoryanalysis.analyzer.ApiEndpointAnalyzer;
import com.example.evidence.backend.repositoryanalysis.exception.RepositoryAnalysisException;
import com.example.evidence.backend.repositoryanalysis.model.*;
import com.example.evidence.backend.repositoryanalysis.scanner.ProjectStructureScanner;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class DefaultRepositoryAnalysisService
        implements RepositoryAnalysisService {

    private final ProjectStructureScanner projectStructureScanner;
    private final DefaultBuildConfigurationAnalyzer mavenAnalyzer;
    private final DefaultGradleConfigurationAnalyzer gradleAnalyzer;
    private final JavaSourceAnalyzer javaSourceAnalyzer;
    private final ApiEndpointAnalyzer apiEndpointAnalyzer;

    public DefaultRepositoryAnalysisService(
            ProjectStructureScanner projectStructureScanner,
            DefaultBuildConfigurationAnalyzer mavenAnalyzer,
            DefaultGradleConfigurationAnalyzer gradleAnalyzer,
            JavaSourceAnalyzer javaSourceAnalyzer,
            ApiEndpointAnalyzer apiEndpointAnalyzer
    ) {
        this.projectStructureScanner = projectStructureScanner;
        this.mavenAnalyzer = mavenAnalyzer;
        this.gradleAnalyzer = gradleAnalyzer;
        this.javaSourceAnalyzer = javaSourceAnalyzer;
        this.apiEndpointAnalyzer = apiEndpointAnalyzer;
    }

    @Override
    public RepositoryAnalysisResult analyze(Path backendRootPath) {

        if (backendRootPath == null ||
                !Files.isDirectory(backendRootPath)) {
            throw new RepositoryAnalysisException(
                    "Invalid backend project directory"
            );
        }

        try {
            // STEP 1: Scan project structure
            ProjectStructure structure =
                    projectStructureScanner.scan(backendRootPath);

            // STEP 2: Analyze build configuration
            BuildConfiguration buildConfiguration =
                    analyzeBuildConfiguration(backendRootPath);

            // STEP 3: Analyze Java source files
            List<JavaClassInfo> javaClasses =
                    javaSourceAnalyzer.analyze(
                            backendRootPath,
                            structure.javaFiles()
                    );

            // STEP 4: Analyze REST endpoints
            List<ApiEndpointInfo> endpoints =
                    apiEndpointAnalyzer.analyze(javaClasses);

            // STEP 5: Consolidate results
            return new RepositoryAnalysisResult(
                    structure,
                    buildConfiguration,
                    javaClasses,
                    endpoints
            );

        } catch (RepositoryAnalysisException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new RepositoryAnalysisException(
                    "Repository analysis failed",
                    exception
            );
        }
    }

    private BuildConfiguration analyzeBuildConfiguration(
            Path backendRootPath
    ) {

        if (Files.isRegularFile(backendRootPath.resolve("pom.xml"))) {
            return mavenAnalyzer.analyze(backendRootPath);
        }

        if (Files.isRegularFile(backendRootPath.resolve("build.gradle"))
                || Files.isRegularFile(
                        backendRootPath.resolve("build.gradle.kts"))) {
            return gradleAnalyzer.analyze(backendRootPath);
        }

        throw new RepositoryAnalysisException(
                "No supported Maven or Gradle build file found"
        );
    }
}
