package com.example.evidence.backend.repositoryanalysis.validator;

import com.example.evidence.backend.repositoryanalysis.dto.CloneRepositoryRequest;
import com.example.evidence.backend.repositoryanalysis.exception.InvalidRepositoryRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultRepositoryRequestValidator Unit Tests")
class DefaultRepositoryRequestValidatorTest {

    private DefaultRepositoryRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultRepositoryRequestValidator();
    }

    @Nested
    @DisplayName("Request Object Validation")
    class RequestObjectTests {

        @Test
        @DisplayName("Should throw exception when request is null")
        void validate_nullRequest_throwsException() {
            assertThatThrownBy(() -> validator.validate(null))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Request cannot be null");
        }
    }

    @Nested
    @DisplayName("Repository URL Validation")
    class RepositoryUrlTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when repository URL is null or blank")
        void validate_blankRepositoryUrl_throwsException(String blankUrl) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(blankUrl, "main", ".");

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Repository URL is required");
        }

        @Test
        @DisplayName("Should pass when URL is a standard valid GitHub URL")
        void validate_validGithubUrl_passes() {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", "."
            );

            assertThatCode(() -> validator.validate(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when valid GitHub URL ends with .git")
        void validate_validGithubUrlWithGitSuffix_passes() {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World.git", "main", "."
            );

            assertThatCode(() -> validator.validate(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when valid GitHub URL has a trailing slash")
        void validate_validGithubUrlWithTrailingSlash_passes() {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World/", "main", "."
            );

            assertThatCode(() -> validator.validate(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when valid GitHub URL has .git and a trailing slash")
        void validate_validGithubUrlWithGitSuffixAndTrailingSlash_passes() {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World.git/", "main", "."
            );

            assertThatCode(() -> validator.validate(request))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://gitlab.com/octocat/Hello-World",
                "https://bitbucket.org/octocat/Hello-World",
                "https://example.com/octocat/Hello-World"
        })
        @DisplayName("Should throw exception for unsupported git hosting domains")
        void validate_unsupportedDomain_throwsException(String unsupportedUrl) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(unsupportedUrl, "main", ".");

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Invalid GitHub repository URL");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://github.com/octocat/Hello-World",
                "ftp://github.com/octocat/Hello-World",
                "ssh://github.com/octocat/Hello-World"
        })
        @DisplayName("Should throw exception when protocol is not HTTPS")
        void validate_nonHttpsScheme_throwsException(String nonHttpsUrl) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(nonHttpsUrl, "main", ".");

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Invalid GitHub repository URL");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://github.com/octocat",
                "https://github.com/",
                "https://github.com",
                "https://github.com/octocat/repo/extra/path",
                "https://github.com/octocat/repo?key=value",
                "https://github.com/octocat/repo#readme",
                "https://user:password@github.com/octocat/repo",
                "https://github.com:8443/octocat/repo"
        })
        @DisplayName("Should throw exception when GitHub URL structure is invalid")
        void validate_invalidUrlStructure_throwsException(String invalidUrl) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(invalidUrl, "main", ".");

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Invalid GitHub repository URL");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "not a uri with spaces",
                "://bad-uri",
                "http://[",
                "git@github.com:octocat/Hello-World.git"
        })
        @DisplayName("Should throw exception when URL string is malformed URI")
        void validate_malformedUri_throwsException(String malformedUrl) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(malformedUrl, "main", ".");

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Malformed repository URL");
        }
    }

    @Nested
    @DisplayName("Branch Validation")
    class BranchValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when branch is null or blank")
        void validate_blankBranch_throwsException(String blankBranch) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", blankBranch, "."
            );

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Branch name is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "main",
                "master",
                "develop",
                "feature/user-auth",
                "release/v1.0.0",
                "bugfix_ISSUE-123",
                "patch-1"
        })
        @DisplayName("Should pass for valid Git branch names")
        void validate_validBranchNames_passes(String validBranch) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", validBranch, "."
            );

            assertThatCode(() -> validator.validate(request))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "branch..name",
                "branch/",
                "/branch",
                "branch.lock",
                "branch?name",
                "branch*name",
                "branch[name",
                "branch~1",
                "branch^1",
                "branch:name",
                "branch\\name",
                "branch@{1}",
                "branch//name"
        })
        @DisplayName("Should throw exception for syntactically invalid Git branch names")
        void validate_invalidBranchNames_throwsException(String invalidBranch) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", invalidBranch, "."
            );

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Invalid Git branch name");
        }
    }

    @Nested
    @DisplayName("Backend Root Directory Validation")
    class BackendRootDirectoryValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when backend root directory is null or blank")
        void validate_blankBackendDirectory_throwsException(String blankDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", blankDir
            );

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Backend root directory is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/backend",
                "/",
                "/src/main",
                "backend\\api",
                "C:/backend",
                "D:/projects",
                "c:backend"
        })
        @DisplayName("Should throw exception when backend root is absolute or contains backslashes/colons")
        void validate_absoluteOrBackslashOrColon_throwsException(String nonRelativeDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", nonRelativeDir
            );

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Backend root must be a relative path");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "../outside",
                "../../etc",
                "backend/../../escaped",
                "a/b/../../../c"
        })
        @DisplayName("Should throw exception when backend root attempts path traversal")
        void validate_pathTraversal_throwsException(String traversalDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", traversalDir
            );

            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(InvalidRepositoryRequestException.class)
                    .hasMessage("Invalid backend root directory");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                ".",
                "backend",
                "services/api",
                "src/main/backend",
                "./backend"
        })
        @DisplayName("Should pass for valid relative backend root directory paths")
        void validate_validRelativePaths_passes(String validDir) {
            CloneRepositoryRequest request = new CloneRepositoryRequest(
                    "https://github.com/octocat/Hello-World", "main", validDir
            );

            assertThatCode(() -> validator.validate(request))
                    .doesNotThrowAnyException();
        }
    }
}
