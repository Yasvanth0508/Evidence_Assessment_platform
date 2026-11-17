package com.example.evidence.backend.repositoryanalysis.validator;

import com.example.evidence.backend.repositoryanalysis.exception.InvalidBackendProjectException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultBackendRootValidator Unit Tests")
class DefaultBackendRootValidatorTest {

    private DefaultBackendRootValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultBackendRootValidator();
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should throw exception when repository root is null")
        void validate_nullRepositoryRoot_throwsException(@TempDir Path tempDir) {
            assertThatThrownBy(() -> validator.validate(null, "backend"))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Repository root and backend directory are required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when backend root directory is null or blank")
        void validate_nullOrBlankBackendDirectory_throwsException(String blankDir, @TempDir Path tempDir) {
            assertThatThrownBy(() -> validator.validate(tempDir, blankDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Repository root and backend directory are required");
        }

        @Test
        @DisplayName("Should throw exception when repository root does not exist")
        void validate_nonExistentRepositoryRoot_throwsException(@TempDir Path tempDir) {
            Path nonExistentRoot = tempDir.resolve("does-not-exist");

            assertThatThrownBy(() -> validator.validate(nonExistentRoot, "backend"))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Unable to validate backend directory");
        }
    }

    @Nested
    @DisplayName("Path Format and Traversal Tests")
    class PathFormatAndTraversalTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "backend\\api",
                "folder\\subfolder",
                "C:/backend",
                "D:/projects/backend"
        })
        @DisplayName("Should throw exception when backend directory is absolute or contains backslashes")
        void validate_nonRelativeOrBackslash_throwsException(String invalidPath, @TempDir Path tempDir) {
            assertThatThrownBy(() -> validator.validate(tempDir, invalidPath))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory must be a relative path");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/backend",
                "/"
        })
        @DisplayName("Should throw exception when backend directory starts with slash")
        void validate_slashPrefixedPath_throwsException(String slashPath, @TempDir Path tempDir) {
            assertThatThrownBy(() -> validator.validate(tempDir, slashPath))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .satisfies(ex -> assertThat(ex.getMessage()).isIn(
                            "Backend directory must be a relative path",
                            "Backend directory cannot escape repository root"
                    ));
        }

        @Test
        @DisplayName("Should throw exception when relative path is absolute system path")
        void validate_absolutePath_throwsException(@TempDir Path tempDir) {
            String absolutePath = tempDir.toAbsolutePath().toString().replace('\\', '/');

            assertThatThrownBy(() -> validator.validate(tempDir, absolutePath))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory must be a relative path");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "../outside",
                "../../other",
                "backend/../../escaped",
                "a/b/../../../root-escape"
        })
        @DisplayName("Should throw exception when backend directory escapes repository root")
        void validate_pathEscapesRoot_throwsException(String escapingPath, @TempDir Path tempDir) {
            assertThatThrownBy(() -> validator.validate(tempDir, escapingPath))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory cannot escape repository root");
        }
    }

    @Nested
    @DisplayName("Filesystem Directory Verification Tests")
    class DirectoryVerificationTests {

        @Test
        @DisplayName("Should resolve and return real path when backend directory exists")
        void validate_existingBackendDirectory_returnsRealPath(@TempDir Path tempDir) throws IOException {
            Path backendDir = Files.createDirectories(tempDir.resolve("backend"));

            Path result = validator.validate(tempDir, "backend");

            assertThat(result).isEqualTo(backendDir.toRealPath());
        }

        @Test
        @DisplayName("Should resolve and return real path when backend directory is nested")
        void validate_nestedBackendDirectory_returnsRealPath(@TempDir Path tempDir) throws IOException {
            Path nestedBackendDir = Files.createDirectories(tempDir.resolve("services/backend-api"));

            Path result = validator.validate(tempDir, "services/backend-api");

            assertThat(result).isEqualTo(nestedBackendDir.toRealPath());
        }

        @Test
        @DisplayName("Should return repository real path when backend directory is '.'")
        void validate_dotBackendDirectory_returnsRepositoryRealPath(@TempDir Path tempDir) throws IOException {
            Path result = validator.validate(tempDir, ".");

            assertThat(result).isEqualTo(tempDir.toRealPath());
        }

        @Test
        @DisplayName("Should throw exception when backend directory does not exist")
        void validate_nonExistentDirectory_throwsException(@TempDir Path tempDir) {
            assertThatThrownBy(() -> validator.validate(tempDir, "nonexistent-dir"))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory does not exist");
        }

        @Test
        @DisplayName("Should throw exception when backend directory path points to a file")
        void validate_pathPointsToFile_throwsException(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("not-a-dir.txt"), "regular file");

            assertThatThrownBy(() -> validator.validate(tempDir, "not-a-dir.txt"))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend root must be a directory");
        }
    }

    @Nested
    @DisplayName("Symbolic Link Tests")
    class SymbolicLinkTests {

        @Test
        @DisplayName("Should throw exception when symlink points outside the repository root")
        void validate_symlinkPointsOutsideRepository_throwsException(@TempDir Path tempDir) throws IOException {
            Path outsideDir = Files.createDirectories(tempDir.resolve("outside-boundary/target-folder"));
            Path repoRoot = Files.createDirectories(tempDir.resolve("repo-root"));
            Path symlink = repoRoot.resolve("symlink-to-outside");

            try {
                Files.createSymbolicLink(symlink, outsideDir);
            } catch (UnsupportedOperationException | FileSystemException | SecurityException e) {
                // On Windows without Developer Mode, symbolic link creation may require elevated privileges
                Assumptions.abort("Filesystem does not support symlink creation without elevated privileges: " + e.getMessage());
            }

            assertThatThrownBy(() -> validator.validate(repoRoot, "symlink-to-outside"))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend directory resolves outside repository root");
        }

        @Test
        @DisplayName("Should pass when symlink points inside the repository root")
        void validate_symlinkPointsInsideRepository_passes(@TempDir Path tempDir) throws IOException {
            Path internalDir = Files.createDirectories(tempDir.resolve("actual-backend"));
            Path symlink = tempDir.resolve("link-to-backend");

            try {
                Files.createSymbolicLink(symlink, internalDir);
            } catch (UnsupportedOperationException | FileSystemException | SecurityException e) {
                Assumptions.abort("Filesystem does not support symlink creation without elevated privileges: " + e.getMessage());
            }

            Path result = validator.validate(tempDir, "link-to-backend");

            assertThat(result).isEqualTo(internalDir.toRealPath());
        }
    }
}
