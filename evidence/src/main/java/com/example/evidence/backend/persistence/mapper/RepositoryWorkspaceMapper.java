
package com.example.evidence.backend.persistence.mapper;

import org.springframework.stereotype.Component;

import com.example.evidence.backend.persistence.entity.Assessment;
import com.example.evidence.backend.persistence.entity.AssessmentWorkspace;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryMetadata;

@Component 
public class RepositoryWorkspaceMapper {

    public AssessmentWorkspace toEntity(
            Assessment assessment,
            RepositoryMetadata metadata
    ) {

        AssessmentWorkspace workspace =
                new AssessmentWorkspace();

        workspace.setAssessment(assessment);

        workspace.setOriginalRepositoryPath(
                metadata.repositoryLocalPath().toString()
        );

        workspace.setCandidateWorkspacePath(null);

        return workspace;
    }
}
