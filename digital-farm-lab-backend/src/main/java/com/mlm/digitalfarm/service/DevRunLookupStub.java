package com.mlm.digitalfarm.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.dto.scenario.RunSnapshot;
import com.mlm.digitalfarm.service.RunLookup;

/**
 * Dev-only stub. Provides a deterministic synthetic run for any ID so the
 * exercise flow can be exercised in isolation. The synthetic user is the
 * seeded learner (Lerato) with ID
 * {@code 11111111-1111-1111-1111-111111111111}.
 *
 * <p>Activate the real implementation with {@code --spring.profiles.active=real}
 * once the scenario module ships one.</p>
 */
@Component
@Profile("stub")
class DevRunLookupStub implements RunLookup {

    private static final UUID SYNTHETIC_LEARNER =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Override
    public RunSnapshot findById(UUID runId) {
        return new RunSnapshot(
                runId,
                UUID.nameUUIDFromBytes("scenario:demo".getBytes()),
                SYNTHETIC_LEARNER,
                // Learner overrode irrigation reduction to 20%
                Map.of(
                        "irrigationReductionPct", 20,
                        "fertiliserKgHa", 120,
                        "plantingDate", "2026-10-15"
                ),
                // Scenario baseline params
                Map.of(
                        "irrigationReductionPct", 0,
                        "fertiliserKgHa", 120,
                        "plantingDate", "2026-10-15"
                ),
                // Results
                Map.of(
                        "yieldTha", 4.8,
                        "waterUseM3Ha", 4200,
                        "costPerHa", 12000,
                        "grossMarginPerHa", 8000
                ),
                // Baseline results
                Map.of(
                        "yieldTha", 5.0,
                        "waterUseM3Ha", 5250,
                        "costPerHa", 13000,
                        "grossMarginPerHa", 7500
                )
        );
    }
}
