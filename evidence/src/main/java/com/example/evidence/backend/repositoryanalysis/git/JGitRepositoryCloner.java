
package com.example.evidence.backend.repositoryanalysis.git;

import com.example.evidence.backend.repositoryanalysis.exception.RepositoryCloneException;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryCloneResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Component
public class JGitRepositoryCloner implements RepositoryCloner {

    @Override
    public RepositoryCloneResult cloneRepository(
            String repositoryUrl,
            String branch,
            Path destination) {

        // 1. Validate destination
        if (destination == null) {
            throw new RepositoryCloneException(
                    "Clone destination cannot be null"
            );
        }

        Path normalizedDestination = destination
                .toAbsolutePath()
                .normalize();

        if (Files.exists(normalizedDestination)) {
            throw new RepositoryCloneException(
                    "Clone destination already exists"
            );
        }

        Path parent = normalizedDestination.getParent();

        if (parent == null) {
            throw new RepositoryCloneException(
                    "Invalid clone destination"
            );
        }

        boolean cloneStarted = false;

        try {

            // 2. Create parent directory
            Files.createDirectories(parent);

            // 3. Clone requested branch
            cloneStarted = true;

            try (Git git = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setBranch(branch)
                    .setBranchesToClone(
                            List.of("refs/heads/" + branch)
                    )
                    .setCloneAllBranches(false)
                    .setDirectory(normalizedDestination.toFile())
                    .setTimeout(120)
                    .call()) {

                // 4. Retrieve checked-out HEAD commit
                ObjectId head = git.getRepository().resolve("HEAD");

                if (head == null) {
                    throw new RepositoryCloneException(
                            "Unable to identify cloned commit"
                    );
                }

                String commitSha = head.getName();

                // 5. Return cloning result
                return new RepositoryCloneResult(
                        normalizedDestination,
                        branch,
                        commitSha
                );
            }

        } catch (GitAPIException | IOException exception) {

            // 6. Clean up incomplete clone
            if (cloneStarted) {
                cleanup(normalizedDestination, exception);
            }

            throw new RepositoryCloneException(
                    "Failed to clone repository: " + exception.getMessage(),
                    exception
            );

        } catch (RuntimeException exception) {

            if (cloneStarted) {
                cleanup(normalizedDestination, exception);
            }

            throw exception;
        }
    }

    private void cleanup(Path destination, Exception originalException) {

        if (!Files.exists(destination)) {
            return;
        }

        try (var paths = Files.walk(destination)) {

            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new CleanupFailure(exception);
                        }
                    });

        } catch (IOException | CleanupFailure exception) {

            originalException.addSuppressed(exception);
        }
    }

    private static class CleanupFailure extends RuntimeException {

        private CleanupFailure(Throwable cause) {
            super(cause);
        }
    }
}
