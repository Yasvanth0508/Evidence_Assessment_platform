
package com.example.evidence.backend.repositoryanalysis.exception;

public class RepositoryAnalysisException extends RuntimeException {

    public RepositoryAnalysisException(String message) {
        super(message);
    }

    public RepositoryAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
