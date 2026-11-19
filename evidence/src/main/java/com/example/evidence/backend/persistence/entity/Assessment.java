
package com.example.evidence.backend.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "assessment",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_assessment_workspace_candidate",
            columnNames = {"workspace_id", "candidate_id"}
        )
    }
)
@Getter
@Setter
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "repository_url", nullable = false, columnDefinition = "TEXT")
    private String repositoryUrl;

    @Column(name = "repository_name")
    private String repositoryName;

    @Column(name = "repository_owner")
    private String repositoryOwner;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "backend_root_directory",
            nullable = false, columnDefinition = "TEXT")
    private String backendRootDirectory;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "scheduled_start_at", nullable = false)
    private Instant scheduledStartAt;

    @Column(nullable = false, length = 30)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
