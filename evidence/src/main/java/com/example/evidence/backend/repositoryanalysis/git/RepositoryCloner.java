
package com.example.evidence.backend.repositoryanalysis.git;

import com.example.evidence.backend.repositoryanalysis.model.RepositoryCloneResult;

import java.nio.file.Path;

public interface RepositoryCloner {

    RepositoryCloneResult cloneRepository(
            String repositoryUrl,
            String branch,
            Path destination
    );
}
