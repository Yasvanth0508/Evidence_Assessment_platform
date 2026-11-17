
package com.example.evidence.backend.repositoryanalysis.validator;

import com.example.evidence.backend.repositoryanalysis.dto.CloneRepositoryRequest;
import com.example.evidence.backend.repositoryanalysis.exception.InvalidRepositoryRequestException;

import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Path;

@Component
public class DefaultRepositoryRequestValidator
        implements RepositoryRequestValidator {

    @Override
    public void validate(CloneRepositoryRequest request) {

        if (request == null) {
            throw new InvalidRepositoryRequestException(
                    "Request cannot be null");
        }

        validateRepositoryUrl(request.repositoryUrl());
        validateBranch(request.branch());
        validateBackendRootDirectory(request.backendRootDirectory());
    }

    private void validateRepositoryUrl(String repositoryUrl) {

        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new InvalidRepositoryRequestException(
                    "Repository URL is required");
        }

        try {
            URI uri = URI.create(repositoryUrl);

            boolean valid = "https".equalsIgnoreCase(uri.getScheme())
                    && "github.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null
                    && uri.getPort() == -1
                    && uri.getQuery() == null
                    && uri.getFragment() == null
                    && uri.getRawPath() != null
                    && uri.getRawPath().matches(
                            "^/[^/]+/[^/]+(?:\\.git)?/?$");

            if (!valid) {
                throw new InvalidRepositoryRequestException(
                        "Invalid GitHub repository URL");
            }

        } catch (IllegalArgumentException exception) {

            throw new InvalidRepositoryRequestException(
                    "Malformed repository URL");

        }
    }

    private void validateBranch(String branch) {

        if (branch == null || branch.isBlank()) {
            throw new InvalidRepositoryRequestException(
                    "Branch name is required");
        }

        boolean valid = Repository.isValidRefName(
                "refs/heads/" + branch);

        if (!valid) {
            throw new InvalidRepositoryRequestException(
                    "Invalid Git branch name");
        }
    }

    private void validateBackendRootDirectory(String directory) {

        if (directory == null || directory.isBlank()) {
            throw new InvalidRepositoryRequestException(
                    "Backend root directory is required");
        }

        if (directory.contains("\\")
                || directory.startsWith("/")
                || directory.contains(":")) {

            throw new InvalidRepositoryRequestException(
                    "Backend root must be a relative path");
        }

        try {
            Path path = Path.of(directory).normalize();

            if (!directory.equals(".")
                    && (path.isAbsolute()
                            || path.startsWith("..")
                            || path.toString().isBlank())) {

                throw new InvalidRepositoryRequestException(
                        "Invalid backend root directory");
            }

        } catch (InvalidRepositoryRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidRepositoryRequestException(
                    "Invalid backend root directory");
        }
    }
}
