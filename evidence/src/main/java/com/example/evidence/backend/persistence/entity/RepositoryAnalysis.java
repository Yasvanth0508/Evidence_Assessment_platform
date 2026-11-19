
package com.example.evidence.backend.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repository_analysis")
@Getter
@Setter
public class RepositoryAnalysis {

    @Id
    @Column(name = "assessment_id")
    private UUID assessmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "project_structure",
            nullable = false, columnDefinition = "jsonb")
    private JsonNode projectStructure;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_code_structure",
            nullable = false, columnDefinition = "jsonb")
    private JsonNode sourceCodeStructure;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_details",
            nullable = false, columnDefinition = "jsonb")
    private JsonNode contentDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
