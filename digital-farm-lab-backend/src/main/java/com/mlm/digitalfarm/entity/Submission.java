package com.mlm.digitalfarm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import com.mlm.digitalfarm.service.RunLookup;

/**
 * A learner's answer to an {@link Exercise}: their scenario {@code run_id},
 * a written reflection, and the deterministic score the engine produced.
 *
 * <p>{@code run_id} is stored as a plain UUID rather than a JPA relation,
 * because the {@code runs} table belongs to the scenario module and we
 * deliberately keep the two modules decoupled (see {@link RunLookup}).</p>
 */
@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false,
            foreignKey = @ForeignKey(name = "submissions_exercise_id_fkey"))
    private Exercise exercise;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "text")
    private String reflection;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Submission() {
        // for JPA
    }

    public Submission(Exercise exercise, UUID runId, UUID userId,
                      int score, String reflection, Instant createdAt) {
        this.exercise = exercise;
        this.runId = runId;
        this.userId = userId;
        this.score = score;
        this.reflection = reflection;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Exercise getExercise() { return exercise; }
    public UUID getRunId() { return runId; }
    public UUID getUserId() { return userId; }
    public int getScore() { return score; }
    public String getReflection() { return reflection; }
    public Instant getCreatedAt() { return createdAt; }

    public void setReflection(String reflection) { this.reflection = reflection; }
}