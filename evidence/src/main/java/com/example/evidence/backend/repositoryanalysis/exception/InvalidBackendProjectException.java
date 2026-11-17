
package com.example.evidence.backend.repositoryanalysis.exception;

public class InvalidBackendProjectException extends RuntimeException {

    public InvalidBackendProjectException(String message) {
        super(message);
    }

    public InvalidBackendProjectException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
