package com.mlm.digitalfarm.controller;

import com.mlm.digitalfarm.dto.scenario.ScenarioParams;
import com.mlm.digitalfarm.dto.scenario.ScenarioResult;
import com.mlm.digitalfarm.service.ScenarioEngine;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Interactive demo of the A14 scenario engine.
 *
 * <p>Called by the Digital Farm Lab dashboard's "Engine" modal. Accepts
 * learner adjustments, runs the deterministic engine for both the adjusted
 * scenario and the field baseline, and returns both so the UI can show a
 * delta.</p>
 *
 * <p>Requires a valid Bearer JWT (enforced by {@code SecurityConfig}), so
 * the server action that calls this must forward a token. That keeps the
 * endpoint safe to leave public — the physics is not a secret, but the API
 * is still access-controlled for defence in depth.</p>
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final ScenarioEngine engine;

    public DemoController(ScenarioEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/run")
    public DemoRunResponse run(@RequestBody(required = false) DemoRunRequest request) {
        // Baseline: MAIZE defaults from BaselineParamsResolver
        ScenarioParams baseline = new ScenarioParams(
                new BigDecimal("550"),
                new BigDecimal("180"),
                LocalDate.of(2026, 10, 15),
                BigDecimal.ZERO);

        ScenarioParams scenario = merge(request, baseline);

        ScenarioResult scenarioResult = engine.run(scenario, baseline);
        ScenarioResult baselineResult = engine.run(baseline, baseline);

        return new DemoRunResponse(scenarioResult, baselineResult);
    }

    private ScenarioParams merge(DemoRunRequest request, ScenarioParams baseline) {
        if (request == null) {
            return baseline;
        }
        BigDecimal irrigation = request.irrigationMm() != null
                ? request.irrigationMm()
                : baseline.irrigationMm();
        BigDecimal fertiliser = request.fertiliserKgHa() != null
                ? request.fertiliserKgHa()
                : baseline.fertiliserKgHa();
        LocalDate planting = request.plantingDate() != null
                ? request.plantingDate()
                : baseline.plantingDate();

        // Derived diagnostic field; used by the exercise rubric, not the engine
        BigDecimal reduction = baseline.irrigationMm().subtract(irrigation)
                .max(BigDecimal.ZERO)
                .divide(baseline.irrigationMm(), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        return new ScenarioParams(irrigation, fertiliser, planting, reduction);
    }

    public record DemoRunRequest(
            BigDecimal irrigationMm,
            BigDecimal fertiliserKgHa,
            LocalDate plantingDate
    ) {}

    public record DemoRunResponse(
            ScenarioResult scenario,
            ScenarioResult baseline
    ) {}
}