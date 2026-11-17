package com.example.evidence.backend.repositoryanalysis.service;

import com.example.evidence.backend.repositoryanalysis.dto.CloneRepositoryRequest;
import com.example.evidence.backend.repositoryanalysis.detector.SpringBootProjectDetector;
import com.example.evidence.backend.repositoryanalysis.git.RepositoryCleanupService;
import com.example.evidence.backend.repositoryanalysis.git.RepositoryCloner;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryCloneResult;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryMetadata;
import com.example.evidence.backend.repositoryanalysis.validator.BackendRootValidator;
import com.example.evidence.backend.repositoryanalysis.validator.RepositoryRequestValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class RepositoryManagerService {

    private final RepositoryRequestValidator requestValidator;
    private final RepositoryCloner repositoryCloner;
    private final BackendRootValidator backendRootValidator;
    private final SpringBootProjectDetector projectDetector;
    private final Path storageRoot;
    private final RepositoryCleanupService cleanupService;


    public RepositoryManagerService(
            RepositoryRequestValidator requestValidator,
            RepositoryCloner repositoryCloner,
            RepositoryCleanupService cleanupService,
            BackendRootValidator backendRootValidator,
            SpringBootProjectDetector projectDetector,
            @Value("${evidence.repositories.root:./data/repositories}") String storageRoot) {
        this.requestValidator = requestValidator;
        this.repositoryCloner = repositoryCloner;
        this.cleanupService = cleanupService;
        this.backendRootValidator = backendRootValidator;
        this.projectDetector = projectDetector;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public RepositoryMetadata analyzeRepository(
            CloneRepositoryRequest request) {

        // 1. Validate request
        requestValidator.validate(request);

        // 2. Generate clone destination
        Path cloneDestination = storageRoot.resolve(
                UUID.randomUUID().toString());

        // 3. Clone repository
        RepositoryCloneResult cloneResult = repositoryCloner.cloneRepository(
                request.repositoryUrl(),
                request.branch(),
                cloneDestination);

        try {

            // 4. Validate backend root
            Path backendRootPath = backendRootValidator.validate(
                    cloneResult.repositoryLocalPath(),
                    request.backendRootDirectory());

            // 5. Detect Spring Boot project
            projectDetector.detect(backendRootPath);

            // 6. Extract repository information
            URI repositoryUri = URI.create(request.repositoryUrl());

            String[] segments = repositoryUri.getPath()
                    .replaceAll("/+$", "")
                    .split("/");

            String owner = segments[1];

            String repositoryName = segments[2]
                    .replaceFirst("\\.git$", "");

            // 7. Return metadata
            return new RepositoryMetadata(
                    repositoryName,
                    owner,
                    request.repositoryUrl(),
                    cloneResult.branch(),
                    cloneResult.commitSha(),
                    cloneResult.repositoryLocalPath(),
                    backendRootPath);

        } catch (RuntimeException exception) {

            // 8. Delete cloned repository after validation failure
            cleanupService.cleanup(cloneDestination, exception);

            throw exception;
        }
    }
}
