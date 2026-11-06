package com.mlm.digitalfarm.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.dto.scenario.RunSnapshot;

/**
 * Port the exercise module uses to read a scenario run.
 *
 * <p>Implement this in the scenario module once the {@code runs} table is
 * wired up. Until then, a synthetic stub bean ({@link DevRunLookupStub}) is
 * active on every profile except {@code real}, so the app starts and demos
 * run end-to-end.</p>
 */
public interface RunLookup {

    /**
     * Fetch a run snapshot.
     *
     * @throws java.util.NoSuchElementException if the run does not exist.
     */
    RunSnapshot findById(UUID runId);
}

