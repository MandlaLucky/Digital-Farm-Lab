package com.mlm.digitalfarm.integration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.Exercise;

/**
 * In-memory implementation of {@link AgriLearnClient} for development, demo
 * and integration tests.
 *
 * <p>Fixture modules mirror the seeded exercises in {@code V2__seed.sql} so
 * the golden journey works without A3 being live. When A3 publishes a real
 * contract, the swap is a profile change — no controller or service changes.</p>
 */
@Component
@Profile("!real")
public class MockAgriLearnClient implements AgriLearnClient {

    private static final Logger log = LoggerFactory.getLogger(MockAgriLearnClient.class);

    /** Same code as the seeded exercise, so the two line up. */
    static final String SEEDED_MODULE_CODE = "LEARN-CROP-01";

    private static final Map<String, AgriLearnClient.Module> FIXTURES = Map.of(
            SEEDED_MODULE_CODE,
            new AgriLearnClient.Module(
                    SEEDED_MODULE_CODE,
                    "Irrigation scenario",
                    "Reduce irrigation by 20% and observe the effect on yield.",
                    List.of(
                            "Introduction to soil water balance",
                            "Reading the scenario parameters",
                            "Running the scenario",
                            "Writing a reflection"),
                    "en",
                    "mock"),

            "LEARN-SOIL-01",
            new AgriLearnClient.Module(
                    "LEARN-SOIL-01",
                    "Soil types and water holding capacity",
                    "Recognise loam, sand and clay from a soil survey and predict water-holding capacity.",
                    List.of("Soil texture triangle", "Field capacity", "Reading a soil test"),
                    "en",
                    "mock"),

            "LEARN-IRRIG-01",
            new AgriLearnClient.Module(
                    "LEARN-IRRIG-01",
                    "Irrigation scheduling basics",
                    "Use evapotranspiration and rainfall to schedule irrigation.",
                    List.of("Reference evapotranspiration", "Crop coefficients", "Effective rainfall"),
                    "en",
                    "mock")
    );

    @PostConstruct
    void announce() {
        log.warn("AgriLearnClient is MOCK — {} fixture module(s) loaded. "
                        + "Activate --spring.profiles.active=real when A3 is available.",
                FIXTURES.size());
    }

    @Override
    public AgriLearnClient.Module fetchModule(String moduleCode) {
        AgriLearnClient.Module module = FIXTURES.get(moduleCode);
        if (module == null) {
            throw new NoSuchElementException(
                    "Mock AgriLearn has no module with code '" + moduleCode + "'");
        }
        log.debug("AgriLearn MOCK hit for module '{}'", moduleCode);
        return module;
    }

    @Override
    public List<AgriLearnClient.Module> listModules() {
        return List.copyOf(FIXTURES.values());
    }
}