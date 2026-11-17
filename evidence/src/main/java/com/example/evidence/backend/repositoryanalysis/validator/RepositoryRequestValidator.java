package com.example.evidence.backend.repositoryanalysis.validator;

import com.example.evidence.backend.repositoryanalysis.dto.CloneRepositoryRequest;

public interface RepositoryRequestValidator {

    void validate(CloneRepositoryRequest request);
}
