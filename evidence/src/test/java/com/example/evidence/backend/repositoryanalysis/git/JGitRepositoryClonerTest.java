package com.example.evidence.backend.repositoryanalysis.git;

import com.example.evidence.backend.repositoryanalysis.exception.RepositoryCloneException;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryCloneResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JGitRepositoryCloner Unit Tests")
class JGitRepositoryClonerTest {

    private JGitRepositoryCloner cloner;

    @BeforeEach
    void setUp() {
        cloner = new JGitRepositoryCloner(new RepositoryCleanupService());
    }

    private String createLocalGitRepository(Path repoDir, String defaultBranch, String commitMessage) throws Exception {
        Files.createDirectories(repoDir);
        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(defaultBranch).call()) {
            Files.writeString(repoDir.resolve("README.md"), "# Test Repo");
            git.add().addFilepattern("README.md").call();
            RevCommit commit = git.commit().setMessage(commitMessage).call();
            return commit.getName();
        }
    }

    @Nested
    @DisplayName("Destination Validation Tests")
    class DestinationValidationTests {

        @Test
        @DisplayName("Should throw exception when destination is null")
        void cloneRepository_nullDestination_throwsException() {
            assertThatThrownBy(() -> cloner.cloneRepository("https://github.com/octocat/Hello-World", "main", null))
                    .isInstanceOf(RepositoryCloneException.class)
                    .hasMessage("Clone destination cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when destination already exists")
        void cloneRepository_existingDestination_throwsException(@TempDir Path tempDir) throws IOException {
            Path existingDir = Files.createDirectories(tempDir.resolve("already-exists"));

            assertThatThrownBy(() -> cloner.cloneRepository(
                    "https://github.com/octocat/Hello-World", "main", existingDir
            ))
                    .isInstanceOf(RepositoryCloneException.class)
                    .hasMessage("Clone destination already exists");
        }

        @Test
        @DisplayName("Should throw exception when destination root has no parent")
        void cloneRepository_destinationHasNoParent_throwsException() {
            Path rootPath = Path.of(File.listRoots()[0].getPath());

            assertThatThrownBy(() -> cloner.cloneRepository(
                    "https://github.com/octocat/Hello-World", "main", rootPath
            ))
                    .isInstanceOf(RepositoryCloneException.class);
        }
    }

    @Nested
    @DisplayName("Cloning Success Tests")
    class CloningSuccessTests {

        @Test
        @DisplayName("Should successfully clone local git repository and return correct metadata")
        void cloneRepository_validRepository_succeeds(@TempDir Path tempDir) throws Exception {
            Path sourceRepoDir = tempDir.resolve("source-repo");
            String commitSha = createLocalGitRepository(sourceRepoDir, "main", "Initial commit on main");

            Path destination = tempDir.resolve("clones/my-clone");
            String sourceUri = sourceRepoDir.toUri().toString();

            RepositoryCloneResult result = cloner.cloneRepository(sourceUri, "main", destination);

            assertThat(result).isNotNull();
            assertThat(result.repositoryLocalPath()).isEqualTo(destination.toAbsolutePath().normalize());
            assertThat(result.branch()).isEqualTo("main");
            assertThat(result.commitSha()).isEqualTo(commitSha);

            // Verify cloned filesystem contents
            assertThat(Files.exists(destination.resolve("README.md"))).isTrue();
            assertThat(Files.readString(destination.resolve("README.md"))).isEqualTo("# Test Repo");
            assertThat(Files.exists(destination.resolve(".git"))).isTrue();
        }

        @Test
        @DisplayName("Should checkout the specified feature branch")
        void cloneRepository_customBranch_checksOutBranch(@TempDir Path tempDir) throws Exception {
            Path sourceRepoDir = tempDir.resolve("source-repo");
            createLocalGitRepository(sourceRepoDir, "main", "Initial commit on main");

            // Create and checkout a second branch in the source repo
            String featureCommitSha;
            try (Git git = Git.open(sourceRepoDir.toFile())) {
                git.branchCreate().setName("feature-v2").call();
                git.checkout().setName("feature-v2").call();
                Files.writeString(sourceRepoDir.resolve("feature.txt"), "new feature");
                git.add().addFilepattern("feature.txt").call();
                RevCommit commit = git.commit().setMessage("Commit on feature-v2").call();
                featureCommitSha = commit.getName();
            }

            Path destination = tempDir.resolve("clones/feature-clone");
            String sourceUri = sourceRepoDir.toUri().toString();

            RepositoryCloneResult result = cloner.cloneRepository(sourceUri, "feature-v2", destination);

            assertThat(result.branch()).isEqualTo("feature-v2");
            assertThat(result.commitSha()).isEqualTo(featureCommitSha);
            assertThat(Files.exists(destination.resolve("feature.txt"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Cloning Failure and Cleanup Tests")
    class FailureAndCleanupTests {

        @Test
        @DisplayName("Should throw exception when repository URL is invalid")
        void cloneRepository_invalidUrl_throwsException(@TempDir Path tempDir) {
            Path destination = tempDir.resolve("failed-clone-invalid-url");

            assertThatThrownBy(() -> cloner.cloneRepository(
                    "file:///path/does/not/exist/or/invalid", "main", destination
            ))
                    .isInstanceOf(RepositoryCloneException.class)
                    .hasMessageStartingWith("Failed to clone repository:");

            // Verify destination directory does not exist or was cleaned up
            assertThat(Files.exists(destination)).isFalse();
        }

        @Test
        @DisplayName("Should throw exception and cleanup when requested branch does not exist")
        void cloneRepository_nonExistentBranch_throwsExceptionAndCleansUp(@TempDir Path tempDir) throws Exception {
            Path sourceRepoDir = tempDir.resolve("source-repo");
            createLocalGitRepository(sourceRepoDir, "main", "Initial commit on main");

            Path destination = tempDir.resolve("failed-clone-bad-branch");
            String sourceUri = sourceRepoDir.toUri().toString();

            assertThatThrownBy(() -> cloner.cloneRepository(sourceUri, "non-existent-branch", destination))
                    .isInstanceOf(RepositoryCloneException.class)
                    .hasMessageStartingWith("Failed to clone repository:");

            // Verify incomplete clone cleanup
            assertThat(Files.exists(destination)).isFalse();
        }
    }
}
