package com.example.evidence.backend.repositoryanalysis.model;

import java.nio.file.Path;

public record RepositoryMetadata(

        String repositoryName,

        String owner,

        String repositoryUrl,

        String branch,

        String commitSha,

        Path repositoryLocalPath,

        Path backendRootPath

) {
}