
package com.example.evidence.backend.repositoryanalysis.model;

import java.nio.file.Path;

public record RepositoryCloneResult(

        Path repositoryLocalPath,

        String branch,

        String commitSha

) {
}
