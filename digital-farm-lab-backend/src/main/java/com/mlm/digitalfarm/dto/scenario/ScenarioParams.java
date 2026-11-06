package com.mlm.digitalfarm.dto.scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.service.RunService;

/**
 * Input parameters for a scenario run. All four fields are stored in the
 * {@code params} JSONB column of {@code scenarios} and {@code runs}.
 *
 * <p>{@code irrigationMm} is what the engine actually consumes. {@code
 * irrigationReductionPct} is a derived/diagnostic field used by the exercise
 * rubric ({@code PARAMS_RANGE}) and by the UI. It has no effect on the
 * engine — that keeps the engine to a single source of truth for water
 * supply.</p>
 *
 * <p>All numeric fields are nullable at the DTO boundary so partial updates
 * work; {@link RunService} merges request overrides onto the scenario's
 * stored params before invoking the engine.</p>
 */
public record ScenarioParams(
        BigDecimal irrigationMm,
        BigDecimal fertiliserKgHa,
        LocalDate plantingDate,
        BigDecimal irrigationReductionPct
) {
    public ScenarioParams {
        if (irrigationMm != null && (irrigationMm.signum() < 0
                || irrigationMm.compareTo(BigDecimal.valueOf(900)) > 0)) {
            throw new IllegalArgumentException("irrigationMm must be in [0, 900]");
        }
        if (fertiliserKgHa != null && (fertiliserKgHa.signum() < 0
                || fertiliserKgHa.compareTo(BigDecimal.valueOf(300)) > 0)) {
            throw new IllegalArgumentException("fertiliserKgHa must be in [0, 300]");
        }
        if (irrigationReductionPct != null && (irrigationReductionPct.signum() < 0
                || irrigationReductionPct.compareTo(BigDecimal.valueOf(50)) > 0)) {
            throw new IllegalArgumentException("irrigationReductionPct must be in [0, 50]");
        }
    }

    /** All fields present — used right before passing to the engine. */
    public ScenarioParams requireComplete() {
        if (irrigationMm == null || fertiliserKgHa == null || plantingDate == null
                || irrigationReductionPct == null) {
            throw new IllegalStateException("ScenarioParams is incomplete");
        }
        return this;
    }
}