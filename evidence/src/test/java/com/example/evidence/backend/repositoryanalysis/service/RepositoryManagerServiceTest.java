package com.example.evidence.backend.repositoryanalysis.service;

import com.example.evidence.backend.repositoryanalysis.detector.SpringBootProjectDetector;
import com.example.evidence.backend.repositoryanalysis.dto.CloneRepositoryRequest;
import com.example.evidence.backend.repositoryanalysis.exception.InvalidBackendProjectException;
import com.example.evidence.backend.repositoryanalysis.exception.InvalidRepositoryRequestException;
import com.example.evidence.backend.repositoryanalysis.exception.RepositoryCloneException;
import com.example.evidence.backend.repositoryanalysis.git.RepositoryCleanupService;
import com.example.evidence.backend.repositoryanalysis.git.RepositoryCloner;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryCloneResult;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryMetadata;
import com.example.evidence.backend.repositoryanalysis.validator.BackendRootValidator;
import com.example.evidence.backend.repositoryanalysis.validator.RepositoryRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryManagerService Unit Tests")
class RepositoryManagerServiceTest {

    @Mock
    private RepositoryRequestValidator requestValidator;

    @Mock
    private RepositoryCloner repositoryCloner;

    @Mock
    private RepositoryCleanupService cleanupService;

    @Mock
    private BackendRootValidator backendRootValidator;

    @Mock
    private SpringBootProjectDetector projectDetector;

    private RepositoryManagerService service;
    private Path storageRoot;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        storageRoot = tempDir.resolve("repositories");
        service = new RepositoryManagerService(
                requestValidator,
                repositoryCloner,
                cleanupService,
                backendRootValidator,
                projectDetector,
                storageRoot.toString()
        );
    }

    @Nested
    @DisplayName("Successful Analysis Tests")
    class SuccessfulAnalysisTests {

        @Test
        @DisplayName("Should successfully analyze repository and build complete metadata")
        void analyzeRepository_validRequest_returnsCompleteMetadata(@TempDir Path tempDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/spring-projects/spring-boot.git",
                    "main",
                    "backend"
            );

            Path localRepoPath = tempDir.resolve("cloned-repo");
            Path backendPath = localRepoPath.resolve("backend");
            String commitSha = "abc123def456";

            when(repositoryCloner.cloneRepository(eq(request.repositoryUrl()), eq(request.branch()), any(Path.class)))
                    .thenReturn(new RepositoryCloneResult(localRepoPath, "main", commitSha));
            when(backendRootValidator.validate(localRepoPath, "backend"))
                    .thenReturn(backendPath);

            RepositoryMetadata metadata = service.analyzeRepository(request);

            assertThat(metadata).isNotNull();
            assertThat(metadata.repositoryName()).isEqualTo("spring-boot");
            assertThat(metadata.owner()).isEqualTo("spring-projects");
            assertThat(metadata.repositoryUrl()).isEqualTo("https://github.com/spring-projects/spring-boot.git");
            assertThat(metadata.branch()).isEqualTo("main");
            assertThat(metadata.commitSha()).isEqualTo(commitSha);
            assertThat(metadata.repositoryLocalPath()).isEqualTo(localRepoPath);
            assertThat(metadata.backendRootPath()).isEqualTo(backendPath);

            // Verify dependency invocation order
            InOrder inOrder = inOrder(requestValidator, repositoryCloner, backendRootValidator, projectDetector);
            inOrder.verify(requestValidator).validate(request);
            inOrder.verify(repositoryCloner).cloneRepository(eq(request.repositoryUrl()), eq(request.branch()), any(Path.class));
            inOrder.verify(backendRootValidator).validate(localRepoPath, "backend");
            inOrder.verify(projectDetector).detect(backendPath);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should generate clone destination under configured storageRoot with UUID")
        void analyzeRepository_destinationGeneratedUnderStorageRoot(@TempDir Path tempDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", "."
            );

            Path localRepoPath = tempDir.resolve("cloned-repo");
            when(repositoryCloner.cloneRepository(any(), any(), any()))
                    .thenReturn(new RepositoryCloneResult(localRepoPath, "main", "sha123"));
            when(backendRootValidator.validate(any(), any()))
                    .thenReturn(localRepoPath);

            service.analyzeRepository(request);

            ArgumentCaptor<Path> destinationCaptor = ArgumentCaptor.forClass(Path.class);
            verify(repositoryCloner).cloneRepository(eq(request.repositoryUrl()), eq("main"), destinationCaptor.capture());

            Path capturedDestination = destinationCaptor.getValue();
            assertThat(capturedDestination.getParent()).isEqualTo(storageRoot.toAbsolutePath().normalize());
            assertThat(capturedDestination.getFileName().toString()).matches("^[0-9a-fA-F\\-]{36}$");
        }

        @ParameterizedTest(name = "URL: {0} -> owner: {1}, repo: {2}")
        @CsvSource({
                "https://github.com/octocat/Hello-World, octocat, Hello-World",
                "https://github.com/octocat/Hello-World.git, octocat, Hello-World",
                "https://github.com/octocat/Hello-World/, octocat, Hello-World",
                "https://github.com/octocat/Hello-World.git/, octocat, Hello-World",
                "https://github.com/org-name/repo-with-dashes.git, org-name, repo-with-dashes",
                "https://github.com/org.name/repo.name.git, org.name, repo.name"
        })
        @DisplayName("Should extract correct owner and repository name across URL variants")
        void analyzeRepository_urlVariants_extractsCorrectOwnerAndName(
                String url, String expectedOwner, String expectedName, @TempDir Path tempDir
        ) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(url, "main", ".");
            Path localRepoPath = tempDir.resolve("cloned-repo");

            when(repositoryCloner.cloneRepository(any(), any(), any()))
                    .thenReturn(new RepositoryCloneResult(localRepoPath, "main", "sha123"));
            when(backendRootValidator.validate(any(), any()))
                    .thenReturn(localRepoPath);

            RepositoryMetadata metadata = service.analyzeRepository(request);

            assertThat(metadata.owner()).isEqualTo(expectedOwner);
            assertThat(metadata.repositoryName()).isEqualTo(expectedName);
        }
    }

    @Nested
    @DisplayName("Failure and Short-Circuit Tests")
    class FailureAndShortCircuitTests {

        @Test
        @DisplayName("Should fail fast when request validation throws exception")
        void analyzeRepository_requestValidationFailure_noFurtherOperations() {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://gitlab.com/octocat/Hello-World", "main", "."
            );

            doThrow(new InvalidRepositoryRequestException("Invalid GitHub repository URL"))
                    .when(requestValidator).validate(request);

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Invalid GitHub repository URL");

            verify(requestValidator).validate(request);
            verifyNoInteractions(repositoryCloner, cleanupService, backendRootValidator, projectDetector);
        }

        @Test
        @DisplayName("Should fail fast when cloning throws exception")
        void analyzeRepository_cloningFailure_noFurtherOperations() {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "ghost-branch", "."
            );

            when(repositoryCloner.cloneRepository(any(), any(), any()))
                    .thenThrow(new RepositoryCloneException("Failed to clone repository: remote branch not found"));

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(RepositoryCloneException.class)
                    .hasMessageContaining("Failed to clone repository");

            verify(requestValidator).validate(request);
            verify(repositoryCloner).cloneRepository(eq(request.repositoryUrl()), eq(request.branch()), any(Path.class));
            verifyNoInteractions(cleanupService, backendRootValidator, projectDetector);
        }

        @Test
        @DisplayName("Should fail fast and trigger cleanup when backend root validation throws exception")
        void analyzeRepository_backendRootValidationFailure_triggersCleanup(@TempDir Path tempDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", "nonexistent-backend"
            );

            Path localRepoPath = tempDir.resolve("cloned-repo");
            when(repositoryCloner.cloneRepository(any(), any(), any()))
                    .thenReturn(new RepositoryCloneResult(localRepoPath, "main", "sha123"));

            when(backendRootValidator.validate(localRepoPath, "nonexistent-backend"))
                    .thenThrow(new InvalidBackendProjectException("Backend directory does not exist"));

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory does not exist");

            verify(requestValidator).validate(request);
            verify(repositoryCloner).cloneRepository(any(), any(), any());
            verify(backendRootValidator).validate(localRepoPath, "nonexistent-backend");
            verify(cleanupService).cleanup(any(Path.class), any(RuntimeException.class));
            verifyNoInteractions(projectDetector);
        }

        @Test
        @DisplayName("Should trigger cleanup and propagate exception when Spring Boot detection throws exception")
        void analyzeRepository_detectionFailure_triggersCleanup(@TempDir Path tempDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", "."
            );

            Path localRepoPath = tempDir.resolve("cloned-repo");
            when(repositoryCloner.cloneRepository(any(), any(), any()))
                    .thenReturn(new RepositoryCloneResult(localRepoPath, "main", "sha123"));
            when(backendRootValidator.validate(localRepoPath, "."))
                    .thenReturn(localRepoPath);

            doThrow(new InvalidBackendProjectException("No Maven or Gradle build configuration found"))
                    .when(projectDetector).detect(localRepoPath);

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("No Maven or Gradle build configuration found");

            verify(requestValidator).validate(request);
            verify(repositoryCloner).cloneRepository(any(), any(), any());
            verify(backendRootValidator).validate(localRepoPath, ".");
            verify(projectDetector).detect(localRepoPath);
            verify(cleanupService).cleanup(any(Path.class), any(RuntimeException.class));
        }

        @Test
        @DisplayName("Verifies post-clone cleanup in RepositoryManagerService passes clone destination")
        void analyzeRepository_postCloneValidationFails_verifiesDestinationPassedToCleanup(@TempDir Path tempDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", "invalid-backend"
            );

            Path localRepoPath = tempDir.resolve("cloned-repo");
            when(repositoryCloner.cloneRepository(any(), any(), any()))
                    .thenReturn(new RepositoryCloneResult(localRepoPath, "main", "sha123"));

            when(backendRootValidator.validate(localRepoPath, "invalid-backend"))
                    .thenThrow(new InvalidBackendProjectException("Backend directory does not exist"));

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory does not exist");

            ArgumentCaptor<Path> destinationCaptor = ArgumentCaptor.forClass(Path.class);
            verify(cleanupService).cleanup(destinationCaptor.capture(), any(RuntimeException.class));
            assertThat(destinationCaptor.getValue().getParent()).isEqualTo(storageRoot.toAbsolutePath().normalize());

            verify(projectDetector, never()).detect(any());
        }
    }
}
