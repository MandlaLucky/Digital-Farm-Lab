package com.mlm.digitalfarm.dto.scenario;

import java.math.BigDecimal;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.service.ScenarioEngine;

/**
 * Output of {@link ScenarioEngine#run(ScenarioParams, ScenarioParams)}.
 *
 * <p>The {@code disclaimer} field is mandatory and is surfaced in every API
 * response and in the PWA. It exists because the model is a teaching
 * artefact, not agronomic advice — a hard rule in {@code AGENTS.md}.</p>
 */
public record ScenarioResult(
        BigDecimal yieldTha,
        BigDecimal waterUseM3Ha,
        BigDecimal costPerHa,
        BigDecimal grossMarginPerHa,
        String disclaimer
) {
    public static final String NOT_AGRONOMIC_ADVICE =
            "Simulated teaching model. NOT agronomic advice.";

    public ScenarioResult {
        if (disclaimer == null || disclaimer.isBlank()) {
            throw new IllegalArgumentException("disclaimer is required");
        }
    }
}