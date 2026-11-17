
package com.example.evidence.backend.repositoryanalysis.validator;

import com.example.evidence.backend.repositoryanalysis.exception.InvalidBackendProjectException;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DefaultBackendRootValidator
        implements BackendRootValidator {

    @Override
    public Path validate(
            Path repositoryRoot,
            String backendRootDirectory) {

        // 1. Validate inputs
        if (repositoryRoot == null
                || backendRootDirectory == null
                || backendRootDirectory.isBlank()) {

            throw new InvalidBackendProjectException(
                    "Repository root and backend directory are required"
            );
        }

        try {

            // 2. Resolve the repository root
            Path realRepositoryRoot = repositoryRoot.toRealPath();

            // 3. Convert the submitted relative path into a Path
            Path relativePath = Path.of(backendRootDirectory);

            // 4. Reject absolute paths
            if (relativePath.isAbsolute()
                    || backendRootDirectory.contains("\\")) {

                throw new InvalidBackendProjectException(
                        "Backend directory must be a relative path"
                );
            }

            // 5. Resolve and normalize the backend directory
            Path resolvedBackendPath = realRepositoryRoot
                    .resolve(relativePath)
                    .normalize();

            // 6. Ensure the path stays inside the repository
            if (!resolvedBackendPath.startsWith(realRepositoryRoot)) {

                throw new InvalidBackendProjectException(
                        "Backend directory cannot escape repository root"
                );
            }

            // 7. Verify that the directory exists
            if (!Files.exists(resolvedBackendPath)) {

                throw new InvalidBackendProjectException(
                        "Backend directory does not exist"
                );
            }

            // 8. Ensure it is actually a directory
            if (!Files.isDirectory(resolvedBackendPath)) {

                throw new InvalidBackendProjectException(
                        "Backend root must be a directory"
                );
            }

            // 9. Resolve symbolic links and verify again
            Path realBackendPath = resolvedBackendPath.toRealPath();

            if (!realBackendPath.startsWith(realRepositoryRoot)) {

                throw new InvalidBackendProjectException(
                        "Backend directory resolves outside repository root"
                );
            }

            // 10. Return verified backend path
            return realBackendPath;

        } catch (InvalidBackendProjectException exception) {

            throw exception;

        } catch (IOException | RuntimeException exception) {

            throw new InvalidBackendProjectException(
                    "Unable to validate backend directory",
                    exception
            );
        }
    }
}
