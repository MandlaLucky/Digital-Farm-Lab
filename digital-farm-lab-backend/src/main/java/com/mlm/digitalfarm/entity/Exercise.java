package com.mlm.digitalfarm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * An exercise wraps a prompt and a machine-scored rubric.
 *
 * <p>The rubric is stored as JSONB (see {@code V1__schema.sql}) so it can
 * evolve without a migration. Its shape is:</p>
 * <pre>
 * {
 *   "criteria": [
 *     { "key": "...", "label": "...", "weight": 40, "type": "PARAMS_RANGE",
 *       "field": "irrigationReductionPct", "min": 15, "max": 25 },
 *     { "key": "...", "label": "...", "weight": 30,
 *       "type": "RESULT_MAX_PCT_DELTA_FROM_BASELINE",
 *       "field": "yieldTha", "maxPctDelta": 0.10 },
 *     { "key": "...", "label": "...", "weight": 20,
 *       "type": "REFLECTION_MIN_WORDS", "minWords": 30 },
 *     { "key": "...", "label": "...", "weight": 10, "type": "PARAMS_DIFFER" }
 *   ]
 * }
 * </pre>
 *
 * <p>The pass threshold is {@link #passScore}, which lives in a column
 * (not in the JSON) so it can be overridden per exercise without
 * rebuilding the rubric.</p>
 */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "module_code", nullable = false, length = 60)
    private String moduleCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rubric;

    @Column(name = "pass_score", nullable = false)
    private int passScore = 60;

    protected Exercise() {
        // for JPA
    }

    public Exercise(String moduleCode, String title, String prompt,
                    Map<String, Object> rubric, int passScore) {
        this.moduleCode = moduleCode;
        this.title = title;
        this.prompt = prompt;
        this.rubric = rubric;
        this.passScore = passScore;
    }

    public UUID getId() { return id; }
    public String getModuleCode() { return moduleCode; }
    public String getTitle() { return title; }
    public String getPrompt() { return prompt; }
    public Map<String, Object> getRubric() { return rubric; }
    public int getPassScore() { return passScore; }

    public void setTitle(String title) { this.title = title; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setRubric(Map<String, Object> rubric) { this.rubric = rubric; }
    public void setPassScore(int passScore) { this.passScore = passScore; }
}