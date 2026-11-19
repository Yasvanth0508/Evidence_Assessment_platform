
package com.example.evidence.backend.persistence.mapper;

import com.example.evidence.backend.persistence.entity.Assessment;
import com.example.evidence.backend.repositoryanalysis.model.RepositoryMetadata;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component 
public class RepositoryMetadataMapper {

    public Assessment toEntity(
            RepositoryMetadata metadata,
            UUID workspaceId,
            UUID candidateId,
            String difficulty,
            Integer durationMinutes,
            Instant scheduledStartAt
    ) {

        Assessment assessment = new Assessment();

        assessment.setWorkspaceId(workspaceId);
        assessment.setCandidateId(candidateId);

        assessment.setRepositoryUrl(metadata.repositoryUrl());
        assessment.setRepositoryName(metadata.repositoryName());
        assessment.setRepositoryOwner(metadata.owner());
        assessment.setBranchName(metadata.branch());
        assessment.setBackendRootDirectory(
                metadata.backendRootPath().toString()
        );
        assessment.setCommitSha(metadata.commitSha());

        assessment.setDifficulty(difficulty);
        assessment.setDurationMinutes(durationMinutes);
        assessment.setScheduledStartAt(scheduledStartAt);
        assessment.setStatus("PREPARING");

        return assessment;
    }
}
