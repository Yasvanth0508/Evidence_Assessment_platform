package com.example.evidence.backend.repositoryanalysis.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RepositoryMetadata Model Unit Tests")
class RepositoryMetadataTest {

    @Test
    @DisplayName("Should correctly store and return record properties")
    void record_accessorsAndProperties() {
        Path repoPath = Path.of("/tmp/repo");
        Path backendPath = Path.of("/tmp/repo/backend");

        RepositoryMetadata metadata = new RepositoryMetadata(
                "my-repo",
                "my-org",
                "https://github.com/my-org/my-repo",
                "develop",
                "sha-456",
                repoPath,
                backendPath
        );

        assertThat(metadata.repositoryName()).isEqualTo("my-repo");
        assertThat(metadata.owner()).isEqualTo("my-org");
        assertThat(metadata.repositoryUrl()).isEqualTo("https://github.com/my-org/my-repo");
        assertThat(metadata.branch()).isEqualTo("develop");
        assertThat(metadata.commitSha()).isEqualTo("sha-456");
        assertThat(metadata.repositoryLocalPath()).isEqualTo(repoPath);
        assertThat(metadata.backendRootPath()).isEqualTo(backendPath);
    }

    @Test
    @DisplayName("Should uphold equality, hashCode, and toString contract")
    void record_equalsHashCodeToString() {
        Path repoPath = Path.of("/tmp/repo");
        Path backendPath = Path.of("/tmp/repo/backend");

        RepositoryMetadata meta1 = new RepositoryMetadata("repo", "org", "https://github.com/org/repo", "main", "sha", repoPath, backendPath);
        RepositoryMetadata meta2 = new RepositoryMetadata("repo", "org", "https://github.com/org/repo", "main", "sha", repoPath, backendPath);
        RepositoryMetadata meta3 = new RepositoryMetadata("repo-diff", "org", "https://github.com/org/repo-diff", "main", "sha", repoPath, backendPath);

        assertThat(meta1).isEqualTo(meta2);
        assertThat(meta1.hashCode()).isEqualTo(meta2.hashCode());
        assertThat(meta1).isNotEqualTo(meta3);
        assertThat(meta1.toString()).contains("repo").contains("org");
    }
}
