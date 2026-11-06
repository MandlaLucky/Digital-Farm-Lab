package com.mlm.digitalfarm.dto.scenario;

import java.util.Map;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.ScenarioRun;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.service.RunLookup;

/**
 * Immutable view of a scenario run, used by the scoring engine.
 *
 * <p>Populated by {@link RunLookup} implementations. Keeping it a record
 * (rather than leaking the {@code ScenarioRun} JPA entity) means:</p>
 * <ul>
 *   <li>the exercise module has zero compile-time dependency on the scenario module;</li>
 *   <li>tests can fabricate snapshots trivially;</li>
 *   <li>when the scenario module is refactored, only the adapter changes.</li>
 * </ul>
 */
public record RunSnapshot(
        UUID runId,
        UUID scenarioId,
        UUID runBy,
        Map<String, Object> runParams,
        Map<String, Object> scenarioParams,
        Map<String, Object> results,
        Map<String, Object> baseline
) {
    public RunSnapshot {
        if (runId == null) {
            throw new IllegalArgumentException("runId is required");
        }
        runParams = runParams == null ? Map.of() : Map.copyOf(runParams);
        scenarioParams = scenarioParams == null ? Map.of() : Map.copyOf(scenarioParams);
        results = results == null ? Map.of() : Map.copyOf(results);
        baseline = baseline == null ? Map.of() : Map.copyOf(baseline);
    }
}