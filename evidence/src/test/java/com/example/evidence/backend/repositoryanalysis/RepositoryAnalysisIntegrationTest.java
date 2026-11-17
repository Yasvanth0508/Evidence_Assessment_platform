package com.example.evidence.backend.repositoryanalysis;

import com.example.evidence.backend.repositoryanalysis.detector.DefaultSpringBootProjectDetector;
import com.example.evidence.backend.repositoryanalysis.detector.SpringBootProjectDetector;
import com.example.evidence.backend.repositoryanalysis.dto.CloneRepositoryRequest;
import com.example.evidence.backend.repositoryanalysis.exception.InvalidBackendProjectException;
import com.example.evidence.backend.repositoryanalysis.git.JGitRepositoryCloner;
import com.example.evidence.backend.repositoryanalysis.git.RepositoryCleanupService;
import com.example.evidence.backend.repositoryanalysis.git.RepositoryCloner;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryMetadata;
import com.example.evidence.backend.repositoryanalysis.service.RepositoryManagerService;
import com.example.evidence.backend.repositoryanalysis.validator.BackendRootValidator;
import com.example.evidence.backend.repositoryanalysis.validator.DefaultBackendRootValidator;
import com.example.evidence.backend.repositoryanalysis.validator.DefaultRepositoryRequestValidator;
import com.example.evidence.backend.repositoryanalysis.validator.RepositoryRequestValidator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Repository Analysis End-to-End Integration Tests")
class RepositoryAnalysisIntegrationTest {

    private RepositoryManagerService service;
    private Path storageRoot;
    private final List<String> registeredInsteadOfUris = new ArrayList<>();

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        RepositoryRequestValidator requestValidator = new DefaultRepositoryRequestValidator();
        RepositoryCleanupService cleanupService = new RepositoryCleanupService();
        RepositoryCloner repositoryCloner = new JGitRepositoryCloner(cleanupService);
        BackendRootValidator backendRootValidator = new DefaultBackendRootValidator();
        SpringBootProjectDetector projectDetector = new DefaultSpringBootProjectDetector();

        storageRoot = tempDir.resolve("storage-root");
        service = new RepositoryManagerService(
                requestValidator,
                repositoryCloner,
                cleanupService,
                backendRootValidator,
                projectDetector,
                storageRoot.toString()
        );
    }

    @AfterEach
    void cleanUpGitConfigs() {
        try {
            StoredConfig userConfig = SystemReader.getInstance().getUserConfig();
            for (String fileUri : registeredInsteadOfUris) {
                userConfig.unsetSection("url", fileUri);
            }
            userConfig.save();
        } catch (Exception ignored) {
        }
    }

    private void registerLocalRepoRedirect(String githubUrl, Path localRepoPath) throws Exception {
        String fileUri = localRepoPath.toUri().toString();
        StoredConfig userConfig = SystemReader.getInstance().getUserConfig();
        userConfig.setString("url", fileUri, "insteadOf", githubUrl);
        userConfig.save();
        registeredInsteadOfUris.add(fileUri);
    }

    @Nested
    @DisplayName("End-to-End Success Scenarios")
    class EndToEndSuccessScenarios {

        @Test
        @DisplayName("Should clone repository, checkout branch, verify backend root, and detect Spring Boot at root")
        void analyzeRepository_rootSpringBootProject_succeeds(@TempDir Path tempDir) throws Exception {
            // 1. Create a minimal valid Spring Boot repository locally
            Path localRepoDir = tempDir.resolve("local-spring-boot-repo");
            Files.createDirectories(localRepoDir);

            Files.writeString(localRepoDir.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>demo-service</artifactId>
                        <version>1.0.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter-web</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """);

            Path javaDir = Files.createDirectories(localRepoDir.resolve("src/main/java/com/example/demo"));
            Files.writeString(javaDir.resolve("DemoApp.java"), """
                    package com.example.demo;
                    import org.springframework.boot.autoconfigure.SpringBootApplication;
                    
                    @SpringBootApplication
                    public class DemoApp {}
                    """);

            String expectedCommitSha;
            try (Git git = Git.init().setDirectory(localRepoDir.toFile()).setInitialBranch("main").call()) {
                git.add().addFilepattern(".").call();
                RevCommit commit = git.commit().setMessage("Initial Spring Boot commit").call();
                expectedCommitSha = commit.getName();
            }

            // 2. Redirect GitHub URL to local repository
            String githubUrl = "https://github.com/acme-corp/demo-service";
            registerLocalRepoRedirect(githubUrl, localRepoDir);

            // 3. Execute analysis
            CloneRepositoryRequest request = new CloneRepositoryRequest(githubUrl, "main", ".");
            RepositoryMetadata metadata = service.analyzeRepository(request);

            // 4. Verify all requirements
            assertThat(metadata).isNotNull();
            assertThat(metadata.repositoryName()).isEqualTo("demo-service");
            assertThat(metadata.owner()).isEqualTo("acme-corp");
            assertThat(metadata.repositoryUrl()).isEqualTo(githubUrl);
            assertThat(metadata.branch()).isEqualTo("main");
            assertThat(metadata.commitSha()).isEqualTo(expectedCommitSha);

            // Verify paths on filesystem
            assertThat(metadata.repositoryLocalPath()).isNotNull();
            assertThat(Files.isDirectory(metadata.repositoryLocalPath())).isTrue();
            assertThat(Files.exists(metadata.repositoryLocalPath().resolve("pom.xml"))).isTrue();
            assertThat(metadata.backendRootPath()).isEqualTo(metadata.repositoryLocalPath().toRealPath());
        }

        @Test
        @DisplayName("Should clone repository with subdirectory backend root and detect Spring Boot in subfolder")
        void analyzeRepository_subDirectoryBackend_succeeds(@TempDir Path tempDir) throws Exception {
            Path localRepoDir = tempDir.resolve("multi-module-repo");
            Files.createDirectories(localRepoDir);

            // Put Spring Boot project in "backend" subfolder
            Path backendDir = Files.createDirectories(localRepoDir.resolve("backend"));
            Files.writeString(backendDir.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>nested-service</artifactId>
                        <version>1.0.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """);

            Path javaDir = Files.createDirectories(backendDir.resolve("src/main/java/com/example/nested"));
            Files.writeString(javaDir.resolve("NestedApp.java"), """
                    package com.example.nested;
                    import org.springframework.boot.autoconfigure.SpringBootApplication;
                    
                    @SpringBootApplication
                    public class NestedApp {}
                    """);

            String expectedCommitSha;
            try (Git git = Git.init().setDirectory(localRepoDir.toFile()).setInitialBranch("main").call()) {
                git.add().addFilepattern(".").call();
                RevCommit commit = git.commit().setMessage("Nested structure commit").call();
                expectedCommitSha = commit.getName();
            }

            String githubUrl = "https://github.com/acme-corp/nested-repo.git";
            registerLocalRepoRedirect(githubUrl, localRepoDir);

            CloneRepositoryRequest request = new CloneRepositoryRequest(githubUrl, "main", "backend");
            RepositoryMetadata metadata = service.analyzeRepository(request);

            assertThat(metadata.repositoryName()).isEqualTo("nested-repo");
            assertThat(metadata.owner()).isEqualTo("acme-corp");
            assertThat(metadata.commitSha()).isEqualTo(expectedCommitSha);
            assertThat(metadata.backendRootPath()).isEqualTo(
                    metadata.repositoryLocalPath().resolve("backend").toRealPath()
            );
        }

        @Test
        @DisplayName("Should checkout requested non-default feature branch and capture feature commit SHA")
        void analyzeRepository_featureBranchCheckout_succeeds(@TempDir Path tempDir) throws Exception {
            Path localRepoDir = tempDir.resolve("branch-test-repo");
            Files.createDirectories(localRepoDir);

            Files.writeString(localRepoDir.resolve("pom.xml"), "<project>spring-boot-starter</project>");
            Path javaDir = Files.createDirectories(localRepoDir.resolve("src/main/java/com/example"));
            Files.writeString(javaDir.resolve("App.java"), "package com.example; @SpringBootApplication public class App {}");

            String featureCommitSha;
            try (Git git = Git.init().setDirectory(localRepoDir.toFile()).setInitialBranch("main").call()) {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("Initial commit on main").call();

                git.branchCreate().setName("feature/v2").call();
                git.checkout().setName("feature/v2").call();
                Files.writeString(localRepoDir.resolve("v2.txt"), "version 2 notes");
                git.add().addFilepattern("v2.txt").call();
                RevCommit commit = git.commit().setMessage("Commit on feature branch").call();
                featureCommitSha = commit.getName();
            }

            String githubUrl = "https://github.com/acme-corp/branch-repo";
            registerLocalRepoRedirect(githubUrl, localRepoDir);

            CloneRepositoryRequest request = new CloneRepositoryRequest(githubUrl, "feature/v2", ".");
            RepositoryMetadata metadata = service.analyzeRepository(request);

            assertThat(metadata.branch()).isEqualTo("feature/v2");
            assertThat(metadata.commitSha()).isEqualTo(featureCommitSha);
            assertThat(Files.exists(metadata.repositoryLocalPath().resolve("v2.txt"))).isTrue();
        }
    }

    @Nested
    @DisplayName("End-to-End Failure Scenarios")
    class EndToEndFailureScenarios {

        @Test
        @DisplayName("Should fail when backend root directory does not exist in cloned repository")
        void analyzeRepository_invalidBackendDirectory_throwsException(@TempDir Path tempDir) throws Exception {
            Path localRepoDir = tempDir.resolve("missing-backend-repo");
            Files.createDirectories(localRepoDir);
            Files.writeString(localRepoDir.resolve("README.md"), "hello");

            try (Git git = Git.init().setDirectory(localRepoDir.toFile()).setInitialBranch("main").call()) {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("init").call();
            }

            String githubUrl = "https://github.com/acme-corp/missing-backend";
            registerLocalRepoRedirect(githubUrl, localRepoDir);

            CloneRepositoryRequest request = new CloneRepositoryRequest(githubUrl, "main", "nonexistent-folder");

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory does not exist");
        }

        @Test
        @DisplayName("Should fail when cloned repository is not a Spring Boot project (missing build config)")
        void analyzeRepository_nonSpringBootRepository_throwsException(@TempDir Path tempDir) throws Exception {
            Path localRepoDir = tempDir.resolve("non-spring-repo");
            Files.createDirectories(localRepoDir);
            Files.writeString(localRepoDir.resolve("app.py"), "print('not java')");

            try (Git git = Git.init().setDirectory(localRepoDir.toFile()).setInitialBranch("main").call()) {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("init python repo").call();
            }

            String githubUrl = "https://github.com/acme-corp/non-spring-repo";
            registerLocalRepoRedirect(githubUrl, localRepoDir);

            CloneRepositoryRequest request = new CloneRepositoryRequest(githubUrl, "main", ".");

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("No Maven or Gradle build configuration found");
        }

        @Test
        @DisplayName("Should fail when build file exists but lacks Spring Boot dependencies")
        void analyzeRepository_plainMavenProject_throwsException(@TempDir Path tempDir) throws Exception {
            Path localRepoDir = tempDir.resolve("plain-maven-repo");
            Files.createDirectories(localRepoDir);
            Files.writeString(localRepoDir.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>plain-java</artifactId>
                        <version>1.0.0</version>
                    </project>
                    """);

            try (Git git = Git.init().setDirectory(localRepoDir.toFile()).setInitialBranch("main").call()) {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("plain maven").call();
            }

            String githubUrl = "https://github.com/acme-corp/plain-maven";
            registerLocalRepoRedirect(githubUrl, localRepoDir);

            CloneRepositoryRequest request = new CloneRepositoryRequest(githubUrl, "main", ".");

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot dependency or plugin not found");
        }

        @Test
        @DisplayName("Should fail when Java source files lack @SpringBootApplication entry point")
        void analyzeRepository_missingSpringBootApplication_throwsException(@TempDir Path tempDir) throws Exception {
            Path localRepoDir = tempDir.resolve("missing-annotation-repo");
            Files.createDirectories(localRepoDir);
            Files.writeString(localRepoDir.resolve("pom.xml"), "<project>spring-boot-starter</project>");
            Path javaDir = Files.createDirectories(localRepoDir.resolve("src/main/java/com/example"));
            Files.writeString(javaDir.resolve("Service.java"), "package com.example; public class Service {}");

            try (Git git = Git.init().setDirectory(localRepoDir.toFile()).setInitialBranch("main").call()) {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("missing annotation").call();
            }

            String githubUrl = "https://github.com/acme-corp/missing-annotation";
            registerLocalRepoRedirect(githubUrl, localRepoDir);

            CloneRepositoryRequest request = new CloneRepositoryRequest(githubUrl, "main", ".");

            assertThatThrownBy(() -> service.analyzeRepository(request))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot application entry point not found");
        }
    }
}
