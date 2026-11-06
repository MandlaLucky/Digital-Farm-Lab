package com.mlm.digitalfarm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "runs")
public class ScenarioRun {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "run_by", nullable = false)
    private UUID runBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> params;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> results;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> baseline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column private Integer score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ScenarioRun() {}

    public ScenarioRun(Scenario scenario, UUID runBy, Map<String, Object> params, Map<String, Object> results, Map<String, Object> baseline) {
        this.scenario = scenario; this.runBy = runBy; this.params = params; this.results = results; this.baseline = baseline;
    }

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Scenario getScenario() { return scenario; }
    public UUID getRunBy() { return runBy; }
    public Map<String, Object> getParams() { return params; }
    public Map<String, Object> getResults() { return results; }
    public Map<String, Object> getBaseline() { return baseline; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Integer getScore() { return score; }
    public Instant getCreatedAt() { return createdAt; }
    public void setScore(Integer score) { this.score = score; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}