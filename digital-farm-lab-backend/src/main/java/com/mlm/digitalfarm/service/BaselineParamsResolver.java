package com.mlm.digitalfarm.service;

import com.mlm.digitalfarm.entity.Field;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.mlm.digitalfarm.entity.Farm;
import com.mlm.digitalfarm.dto.scenario.ScenarioParams;

/**
 * Derives the baseline parameters for a field from its crop code.
 *
 * <p>Baselines are synthetic defaults that represent "the farm's normal
 * practice". They are not stored on {@code Field} because that would need a
 * schema change; instead they are computed deterministically at run time and
 * frozen into {@code runs.baseline} on every run. If the field's crop changes,
 * old runs still reflect the baseline they were created against.</p>
 *
 * <p>Values are illustrative and documented in {@code AGENTS.md}. They are
 * not agronomic recommendations.</p>
 */
@Component
public class BaselineParamsResolver {

    public ScenarioParams forField(Field field) {
        String crop = field.getCropCode() == null ? "" : field.getCropCode().trim().toUpperCase();
        LocalDate planting = field.getPlantingDate() == null
                ? LocalDate.of(LocalDate.now().getYear(), 10, 15)
                : field.getPlantingDate();

        return switch (crop) {
            case "MAIZE" -> new ScenarioParams(
                    new BigDecimal("550"),
                    new BigDecimal("180"),
                    planting,
                    BigDecimal.ZERO);
            default -> new ScenarioParams(
                    new BigDecimal("400"),
                    new BigDecimal("100"),
                    planting,
                    BigDecimal.ZERO);
        };
    }
}