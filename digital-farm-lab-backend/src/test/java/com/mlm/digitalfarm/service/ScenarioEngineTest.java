package com.mlm.digitalfarm.service;

import com.mlm.digitalfarm.dto.scenario.ScenarioParams;
import com.mlm.digitalfarm.dto.scenario.ScenarioResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the deterministic scenario engine.
 * No Spring context, no DB — these are the viva proof that the model
 * is consistent, monotone in the right direction, and honest about limits.
 */
class ScenarioEngineTest {

    private ScenarioEngine engine;

    @BeforeEach
    void setUp() { engine = new ScenarioEngine(); }

    /** Baseline = MAIZE defaults from BaselineParamsResolver. */
    private ScenarioParams baseline() {
        return new ScenarioParams(
                new BigDecimal("550"),
                new BigDecimal("180"),
                LocalDate.of(2026, 10, 15),
                BigDecimal.ZERO);
    }

    private ScenarioParams scenario(String irr, String fert, LocalDate date, String reduction) {
        return new ScenarioParams(new BigDecimal(irr), new BigDecimal(fert), date, new BigDecimal(reduction));
    }

    // ---------- Determinism ----------

    @Test
    @DisplayName("Engine is deterministic: same inputs produce equal outputs")
    void deterministic() {
        assertThat(engine.run(baseline(), baseline()))
                .isEqualTo(engine.run(baseline(), baseline()));
    }

    // ---------- Baseline values (locked to engine constants) ----------

    @Test
    @DisplayName("Baseline yield is ~6.92 t/ha")
    void baselineYield() {
        assertThat(engine.run(baseline(), baseline()).yieldTha())
                .isBetween(new BigDecimal("6.85"), new BigDecimal("7.00"));
    }

    @Test
    @DisplayName("Baseline water use is exactly 7000 m3/ha")
    void baselineWater() {
        assertThat(engine.run(baseline(), baseline()).waterUseM3Ha())
                .isEqualByComparingTo("7000.00");
    }

    @Test
    @DisplayName("Baseline cost is exactly R12035/ha")
    void baselineCost() {
        assertThat(engine.run(baseline(), baseline()).costPerHa())
                .isEqualByComparingTo("12035.00");
    }

    // ---------- Monotonicity: less input -> less output ----------

    @Test
    @DisplayName("20% irrigation reduction lowers yield below baseline")
    void lessWaterLessYield() {
        ScenarioParams s = scenario("440", "180", LocalDate.of(2026, 10, 15), "20");
        assertThat(engine.run(s, baseline()).yieldTha())
                .isLessThan(engine.run(baseline(), baseline()).yieldTha());
    }

    @Test
    @DisplayName("Less irrigation -> less water consumption")
    void lessWaterLessConsumption() {
        ScenarioParams s = scenario("440", "180", LocalDate.of(2026, 10, 15), "20");
        assertThat(engine.run(s, baseline()).waterUseM3Ha())
                .isLessThan(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("Less fertiliser -> not more yield")
    void lessFertiliserNotMoreYield() {
        ScenarioParams low = scenario("550", "50", LocalDate.of(2026, 10, 15), "0");
        assertThat(engine.run(low, baseline()).yieldTha())
                .isLessThanOrEqualTo(engine.run(baseline(), baseline()).yieldTha());
    }

    // ---------- Edge cases ----------

    @Test
    @DisplayName("Zero fertiliser -> zero yield (Mitscherlich response)")
    void zeroFertiliser() {
        ScenarioParams p = scenario("550", "0", LocalDate.of(2026, 10, 15), "0");
        assertThat(engine.run(p, baseline()).yieldTha()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Max irrigation (900mm) + max fertiliser stays under 8 t/ha ceiling")
    void maxIrrigationCapped() {
        ScenarioParams p = scenario("900", "300", LocalDate.of(2026, 10, 15), "0");
        assertThat(engine.run(p, baseline()).yieldTha())
                .isLessThanOrEqualTo(new BigDecimal("8.00"));
    }

    @Test
    @DisplayName("Planting 30 days after optimum incurs a yield penalty")
    void latePlantingPenalty() {
        ScenarioParams late = scenario("550", "180", LocalDate.of(2026, 11, 14), "0");
        assertThat(engine.run(late, baseline()).yieldTha())
                .isLessThan(engine.run(baseline(), baseline()).yieldTha());
    }

    // ---------- Contract ----------

    @Test
    @DisplayName("Disclaimer is always present and correct")
    void disclaimerAlwaysPresent() {
        assertThat(engine.run(baseline(), baseline()).disclaimer())
                .isEqualTo(ScenarioResult.NOT_AGRONOMIC_ADVICE);
    }

    @Test
    @DisplayName("Incomplete params throw — no silent zeros")
    void incompleteParamsThrows() {
        ScenarioParams incomplete = new ScenarioParams(
                null, new BigDecimal("180"), LocalDate.of(2026, 10, 15), BigDecimal.ZERO);
        assertThatThrownBy(() -> engine.run(incomplete, baseline()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incomplete");
    }
}