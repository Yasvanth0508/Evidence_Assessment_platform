
package com.example.evidence.backend.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_workspace")
@Getter
@Setter
public class AssessmentWorkspace {

    @Id
    @Column(name = "assessment_id")
    private UUID assessmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @Column(name = "original_repository_path", columnDefinition = "TEXT")
    private String originalRepositoryPath;

    @Column(name = "candidate_workspace_path", columnDefinition = "TEXT")
    private String candidateWorkspacePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
