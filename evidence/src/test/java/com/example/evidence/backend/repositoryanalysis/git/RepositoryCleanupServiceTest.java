package com.example.evidence.backend.repositoryanalysis.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RepositoryCleanupService Unit Tests")
class RepositoryCleanupServiceTest {

    private RepositoryCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new RepositoryCleanupService();
    }

    @Test
    @DisplayName("Should cleanly delete existing directory and all its contents")
    void cleanup_existingDirectory_deletesAllFiles(@TempDir Path tempDir) throws IOException {
        Path targetDir = Files.createDirectories(tempDir.resolve("repo-to-clean/subdir"));
        Files.writeString(targetDir.resolve("file.txt"), "sample data");
        Files.writeString(tempDir.resolve("repo-to-clean/root.txt"), "root data");

        Path rootToClean = tempDir.resolve("repo-to-clean");
        assertThat(Files.exists(rootToClean)).isTrue();

        Exception dummyException = new RuntimeException("Original error");
        cleanupService.cleanup(rootToClean, dummyException);

        assertThat(Files.exists(rootToClean)).isFalse();
        assertThat(dummyException.getSuppressed()).isEmpty();
    }

    @Test
    @DisplayName("Should be a no-op when target path does not exist")
    void cleanup_nonExistentDirectory_noOp(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does-not-exist");
        Exception dummyException = new RuntimeException("Original error");

        cleanupService.cleanup(nonExistent, dummyException);

        assertThat(Files.exists(nonExistent)).isFalse();
        assertThat(dummyException.getSuppressed()).isEmpty();
    }
}
