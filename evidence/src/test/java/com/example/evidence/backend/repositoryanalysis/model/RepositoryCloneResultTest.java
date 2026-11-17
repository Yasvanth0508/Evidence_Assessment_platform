package com.example.evidence.backend.repositoryanalysis.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RepositoryCloneResult Model Unit Tests")
class RepositoryCloneResultTest {

    @Test
    @DisplayName("Should correctly store and return record properties")
    void record_accessorsAndProperties() {
        Path path = Path.of("/tmp/repo");
        RepositoryCloneResult result = new RepositoryCloneResult(path, "main", "commit-sha-123");

        assertThat(result.repositoryLocalPath()).isEqualTo(path);
        assertThat(result.branch()).isEqualTo("main");
        assertThat(result.commitSha()).isEqualTo("commit-sha-123");
    }

    @Test
    @DisplayName("Should uphold equality, hashCode, and toString contract")
    void record_equalsHashCodeToString() {
        Path path1 = Path.of("/tmp/repo1");
        Path path2 = Path.of("/tmp/repo2");

        RepositoryCloneResult result1 = new RepositoryCloneResult(path1, "main", "sha-1");
        RepositoryCloneResult result2 = new RepositoryCloneResult(path1, "main", "sha-1");
        RepositoryCloneResult result3 = new RepositoryCloneResult(path2, "main", "sha-1");

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        assertThat(result1).isNotEqualTo(result3);
        assertThat(result1.toString()).contains("main").contains("sha-1");
    }
}
