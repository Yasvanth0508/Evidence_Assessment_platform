package com.example.evidence.backend.repositoryanalysis.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Repository Analysis Custom Exceptions Unit Tests")
class RepositoryAnalysisExceptionsTest {

    @Test
    @DisplayName("InvalidRepositoryRequestException should retain message")
    void invalidRepositoryRequestException_messageConstructor() {
        InvalidRepositoryRequestException exception = new InvalidRepositoryRequestException("Validation error message");

        assertThat(exception.getMessage()).isEqualTo("Validation error message");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("InvalidBackendProjectException should support message and cause")
    void invalidBackendProjectException_constructors() {
        InvalidBackendProjectException exceptionWithMessage = new InvalidBackendProjectException("Backend error message");
        assertThat(exceptionWithMessage.getMessage()).isEqualTo("Backend error message");
        assertThat(exceptionWithMessage.getCause()).isNull();

        IllegalArgumentException cause = new IllegalArgumentException("Root cause");
        InvalidBackendProjectException exceptionWithCause = new InvalidBackendProjectException("Backend error", cause);
        assertThat(exceptionWithCause.getMessage()).isEqualTo("Backend error");
        assertThat(exceptionWithCause.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("RepositoryCloneException should support message and cause")
    void repositoryCloneException_constructors() {
        RepositoryCloneException exceptionWithMessage = new RepositoryCloneException("Clone error message");
        assertThat(exceptionWithMessage.getMessage()).isEqualTo("Clone error message");
        assertThat(exceptionWithMessage.getCause()).isNull();

        IllegalStateException cause = new IllegalStateException("Transport failed");
        RepositoryCloneException exceptionWithCause = new RepositoryCloneException("Clone error", cause);
        assertThat(exceptionWithCause.getMessage()).isEqualTo("Clone error");
        assertThat(exceptionWithCause.getCause()).isSameAs(cause);
    }
}
