package com.mlm.digitalfarm.service;

import com.mlm.digitalfarm.service.RunLookup;
import com.mlm.digitalfarm.dto.scenario.RunSnapshot;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.mlm.digitalfarm.entity.ScenarioRun;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.entity.Submission;
import com.mlm.digitalfarm.repository.ScenarioRunRepository;

/**
 * Real implementation of the exercise module's {@link RunLookup} port.
 * Wires the exercise submission flow to actual persisted runs.
 *
 * <p>Active on every profile except {@code stub}. The stub is kept for
 * isolated exercise-module tests where a real DB is not available.</p>
 */
@Component
@Profile("!stub")
@Transactional(readOnly = true)
public class ScenarioRunLookup implements RunLookup {

    private final ScenarioRunRepository runs;

    public ScenarioRunLookup(ScenarioRunRepository runs) {
        this.runs = runs;
    }

    @Override
    public RunSnapshot findById(UUID runId) {
        ScenarioRun run = runs.findById(runId)
                .orElseThrow(() -> new NoSuchElementException("Run not found: " + runId));

        return new RunSnapshot(
                run.getId(),
                run.getScenario().getId(),
                run.getRunBy(),
                asStringMap(run.getParams()),
                asStringMap(run.getScenario().getParams()),
                asStringMap(run.getResults()),
                asStringMap(run.getBaseline()));
    }

    private Map<String, Object> asStringMap(Map<String, Object> raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, Object> out = new HashMap<>(raw.size());
        raw.forEach((k, v) -> out.put(String.valueOf(k), v));
        return out;
    }
}