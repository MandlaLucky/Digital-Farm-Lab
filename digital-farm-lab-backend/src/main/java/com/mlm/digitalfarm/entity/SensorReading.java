package com.mlm.digitalfarm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single synthetic sensor reading for a field. The DB enforces
 * {@code UNIQUE(field_id, type, recorded_at)} so re-running the seed
 * is idempotent (see {@code V1__schema.sql}).
 */
@Entity
@Table(name = "sensor_readings")
public class SensorReading {

    public enum Type {
        SOIL_MOISTURE,
        RAIN_MM,
        TEMP_C
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false,
            foreignKey = @ForeignKey(name = "sensor_readings_field_id_fkey"))
    private Field field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal value;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected SensorReading() {
        // for JPA
    }

    public SensorReading(Field field, Type type, BigDecimal value, Instant recordedAt) {
        this.field = field;
        this.type = type;
        this.value = value;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public Field getField() { return field; }
    public Type getType() { return type; }
    public BigDecimal getValue() { return value; }
    public Instant getRecordedAt() { return recordedAt; }
}