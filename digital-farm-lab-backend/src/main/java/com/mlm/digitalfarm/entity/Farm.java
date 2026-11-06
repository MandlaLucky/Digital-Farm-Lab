package com.mlm.digitalfarm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "farms")
public class Farm {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 60)
    private String province;

    @Column(name = "total_ha", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHa;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Farm() {}

    public Farm(UUID ownerId, String name, String province, BigDecimal totalHa) {
        this.ownerId = ownerId;
        this.name = name;
        this.province = province;
        this.totalHa = totalHa;
    }

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getProvince() { return province; }
    public BigDecimal getTotalHa() { return totalHa; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setName(String name) { this.name = name; }
    public void setProvince(String province) { this.province = province; }
    public void setTotalHa(BigDecimal totalHa) { this.totalHa = totalHa; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}