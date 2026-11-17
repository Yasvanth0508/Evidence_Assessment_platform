
package com.example.evidence.backend.repositoryanalysis.validator;

import java.nio.file.Path;

public interface BackendRootValidator {

    Path validate(
            Path repositoryRoot,
            String backendRootDirectory
    );
}
