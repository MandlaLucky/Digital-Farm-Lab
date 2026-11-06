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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.mlm.digitalfarm.repository.FieldRepository;

/**
 * A field is a parcel of a {@link Farm}. Only the many-side is mapped;
 * there is no {@code Farm.fields} collection, so no accidental N+1 and no
 * lazy-loading traps under {@code open-in-view: false}. Use
 * {@code FieldRepository.findByFarm_IdAndFarm_OwnerId} to list a farm's fields.
 */
@Entity
@Table(name = "fields")
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false,
            foreignKey = @ForeignKey(name = "fields_farm_id_fkey"))
    private Farm farm;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "area_ha", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaHa;

    @Column(name = "soil_type", nullable = false, length = 40)
    private String soilType;

    @Column(name = "crop_code", nullable = false, length = 40)
    private String cropCode;

    @Column(name = "planting_date", nullable = false)
    private LocalDate plantingDate;

    protected Field() {
        // for JPA
    }

    public Field(Farm farm, String name, BigDecimal areaHa,
                 String soilType, String cropCode, LocalDate plantingDate) {
        this.farm = farm;
        this.name = name;
        this.areaHa = areaHa;
        this.soilType = soilType;
        this.cropCode = cropCode;
        this.plantingDate = plantingDate;
    }

    public UUID getId() { return id; }
    public Farm getFarm() { return farm; }
    public String getName() { return name; }
    public BigDecimal getAreaHa() { return areaHa; }
    public String getSoilType() { return soilType; }
    public String getCropCode() { return cropCode; }
    public LocalDate getPlantingDate() { return plantingDate; }

    public void setName(String name) { this.name = name; }
    public void setAreaHa(BigDecimal areaHa) { this.areaHa = areaHa; }
    public void setSoilType(String soilType) { this.soilType = soilType; }
    public void setCropCode(String cropCode) { this.cropCode = cropCode; }
    public void setPlantingDate(LocalDate plantingDate) { this.plantingDate = plantingDate; }
}