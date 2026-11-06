package com.mlm.digitalfarm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "scenarios")
public class Scenario {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false, length = 160)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> params;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Scenario() {}

    public Scenario(Field field, UUID createdBy, String name, Map<String, Object> params) {
        this.field = field; this.createdBy = createdBy; this.name = name; this.params = params;
    }

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Field getField() { return field; }
    public UUID getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public Map<String, Object> getParams() { return params; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setName(String name) { this.name = name; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}