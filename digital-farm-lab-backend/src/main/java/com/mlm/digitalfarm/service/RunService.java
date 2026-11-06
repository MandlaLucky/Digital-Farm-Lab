package com.mlm.digitalfarm.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.mlm.digitalfarm.entity.Field;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.ScenarioRun;
import com.mlm.digitalfarm.repository.ScenarioRepository;
import com.mlm.digitalfarm.repository.ScenarioRunRepository;
import com.mlm.digitalfarm.dto.scenario.ScenarioParams;
import com.mlm.digitalfarm.dto.scenario.ScenarioResult;

/**
 * Orchestrates scenario runs.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Enforce owner scoping.</li>
 *   <li>Merge request overrides onto the scenario's stored params.</li>
 *   <li>Derive the field's baseline params and the derived irrigation
 *       reduction percentage.</li>
 *   <li>Call {@link ScenarioEngine} twice (scenario + baseline).</li>
 *   <li>Persist the {@link ScenarioRun} with params, results and baseline.</li>
 * </ul>
 */
@Service
public class RunService {

    private final ScenarioRepository scenarios;
    private final ScenarioRunRepository runs;
    private final ScenarioEngine engine;
    private final BaselineParamsResolver baselineResolver;
    private final ObjectMapper objectMapper;

    public RunService(ScenarioRepository scenarios,
                      ScenarioRunRepository runs,
                      ScenarioEngine engine,
                      BaselineParamsResolver baselineResolver,
                      ObjectMapper objectMapper) {
        this.scenarios = scenarios;
        this.runs = runs;
        this.engine = engine;
        this.baselineResolver = baselineResolver;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Scenario loadScenario(UUID scenarioId, UUID userId) {
        return scenarios.findByIdAndCreatedByAndField_Farm_OwnerId(scenarioId, userId, userId)
                .orElseThrow(() -> new NoSuchElementException("Scenario not found: " + scenarioId));
    }

    @Transactional(readOnly = true)
    public List<Scenario> listScenarios(UUID userId) {
        return scenarios.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ScenarioRun> listRuns(UUID scenarioId, UUID userId) {
        // Ensure the scenario exists and is owned before listing.
        loadScenario(scenarioId, userId);
        return runs.findByScenario_IdOrderByCreatedAtDesc(scenarioId);
    }

    @Transactional(readOnly = true)
    public ScenarioRun loadRun(UUID scenarioId, UUID runId, UUID userId) {
        ScenarioRun run = runs.findByIdAndScenario_CreatedByAndScenario_Field_Farm_OwnerId(
                        runId, userId, userId)
                .orElseThrow(() -> new NoSuchElementException("Run not found: " + runId));
        if (!run.getScenario().getId().equals(scenarioId)) {
            throw new NoSuchElementException("Run " + runId + " does not belong to scenario " + scenarioId);
        }
        return run;
    }

    // ------------------------------------------------------------------
    // Commands
    // ------------------------------------------------------------------

    @Transactional
    public Scenario createScenario(Field field, UUID userId, String name, ScenarioParams initial) {
        ScenarioParams baselineParams = baselineResolver.forField(field);
        ScenarioParams merged = merge(initial, baselineParams, baselineParams);
        ScenarioParams completed = merged.requireComplete();

        Scenario scenario = new Scenario(
                field, userId, name, toMap(completed));
        return scenarios.save(scenario);
    }

    @Transactional
    public ScenarioRun run(UUID scenarioId, UUID userId, ScenarioParams overrides) {
        Scenario scenario = loadScenario(scenarioId, userId);
        Field field = scenario.getField();

        ScenarioParams baselineParams = baselineResolver.forField(field);
        ScenarioParams storedParams = fromMap(scenario.getParams());
        ScenarioParams effective = merge(overrides, storedParams, baselineParams).requireComplete();

        ScenarioResult scenarioResult = engine.run(effective, baselineParams);
        ScenarioResult baselineResult = engine.run(baselineParams, baselineParams);

        ScenarioRun run = new ScenarioRun(
                scenario,
                userId,
                toMap(effective),
                toMap(scenarioResult),
                toMap(baselineResult));

        return runs.save(run);
    }

    // ------------------------------------------------------------------
    // Merge and conversion
    // ------------------------------------------------------------------

    /**
     * Apply request overrides on top of the scenario's stored params, defaulting
     * any still-missing field to the baseline. Also derives
     * {@code irrigationReductionPct} from the actual irrigation values if it is
     * missing.
     */
    private ScenarioParams merge(ScenarioParams overrides,
                                 ScenarioParams stored,
                                 ScenarioParams baseline) {
        BigDecimal irrigation = firstNonNull(
                overrides == null ? null : overrides.irrigationMm(),
                stored == null ? null : stored.irrigationMm(),
                baseline.irrigationMm());

        BigDecimal fertiliser = firstNonNull(
                overrides == null ? null : overrides.fertiliserKgHa(),
                stored == null ? null : stored.fertiliserKgHa(),
                baseline.fertiliserKgHa());

        LocalDate planting = overrides != null && overrides.plantingDate() != null
                ? overrides.plantingDate()
                : stored != null && stored.plantingDate() != null
                ? stored.plantingDate()
                : baseline.plantingDate();

        BigDecimal reduction = overrides != null ? overrides.irrigationReductionPct() : null;
        if (reduction == null) {
            reduction = stored != null ? stored.irrigationReductionPct() : null;
        }
        if (reduction == null && baseline.irrigationMm() != null
                && baseline.irrigationMm().signum() > 0) {
            BigDecimal diff = baseline.irrigationMm().subtract(irrigation);
            reduction = diff.max(BigDecimal.ZERO)
                    .divide(baseline.irrigationMm(), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP);
        }
        if (reduction == null) {
            reduction = BigDecimal.ZERO;
        }

        return new ScenarioParams(irrigation, fertiliser, planting, reduction);
    }

    private static <T> T firstNonNull(T a, T b, T c) {
        if (a != null) return a;
        if (b != null) return b;
        return c;
    }

    private Map<String, Object> toMap(ScenarioParams params) {
        Map<String, Object> raw = objectMapper.convertValue(params, new TypeReference<>() {});
        // Drop nulls so JSONB stays compact and equality checks stay reliable.
        Map<String, Object> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> {
            if (v != null) out.put(k, v);
        });
        return out;
    }

    private Map<String, Object> toMap(ScenarioResult result) {
        Map<String, Object> raw = objectMapper.convertValue(result, new TypeReference<>() {});
        Map<String, Object> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> {
            if (v != null) out.put(k, v);
        });
        return out;
    }

    private ScenarioParams fromMap(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ScenarioParams(null, null, null, null);
        }
        return objectMapper.convertValue(raw, ScenarioParams.class);
    }
}