package com.example.evidence.backend.repositoryanalysis.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CloneRepositoryRequest DTO Unit Tests")
class CloneRepositoryRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should pass validation when all fields are populated correctly")
    void record_validFields_hasNoViolations() {
        CloneRepositoryRequest request = new CloneRepositoryRequest(
                "https://github.com/octocat/Hello-World", "main", "backend"
        );

        Set<ConstraintViolation<CloneRepositoryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.repositoryUrl()).isEqualTo("https://github.com/octocat/Hello-World");
        assertThat(request.branch()).isEqualTo("main");
        assertThat(request.backendRootDirectory()).isEqualTo("backend");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should detect constraint violation when repositoryUrl is blank or null")
    void record_blankRepositoryUrl_hasViolation(String url) {
        CloneRepositoryRequest request = new CloneRepositoryRequest(url, "main", "backend");

        Set<ConstraintViolation<CloneRepositoryRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Repository URL is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should detect constraint violation when branch is blank or null")
    void record_blankBranch_hasViolation(String branch) {
        CloneRepositoryRequest request = new CloneRepositoryRequest(
                "https://github.com/octocat/Hello-World", branch, "backend"
        );

        Set<ConstraintViolation<CloneRepositoryRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Branch name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should detect constraint violation when backendRootDirectory is blank or null")
    void record_blankBackendDirectory_hasViolation(String backendDir) {
        CloneRepositoryRequest request = new CloneRepositoryRequest(
                "https://github.com/octocat/Hello-World", "main", backendDir
        );

        Set<ConstraintViolation<CloneRepositoryRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Backend root directory is required");
    }

    @Test
    @DisplayName("Should uphold equality and hashCode contract")
    void record_equalsAndHashCode() {
        CloneRepositoryRequest request1 = new CloneRepositoryRequest("https://github.com/a/b", "main", ".");
        CloneRepositoryRequest request2 = new CloneRepositoryRequest("https://github.com/a/b", "main", ".");
        CloneRepositoryRequest request3 = new CloneRepositoryRequest("https://github.com/a/c", "main", ".");

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.toString()).contains("https://github.com/a/b");
    }
}
