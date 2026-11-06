package com.mlm.digitalfarm.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.dto.scenario.ScenarioParams;
import com.mlm.digitalfarm.dto.scenario.ScenarioResult;

/**
 * Deterministic, pure scenario engine.
 *
 * <p><strong>Purity contract:</strong> no I/O, no clocks, no randomness, no
 * Spring injection. Every input is a parameter; every output is a {@link
 * ScenarioResult}. Running the same inputs twice produces the same outputs
 * on any JVM within 1 ULP (we use {@link Math#exp} once, for the Mitscherlich
 * response).</p>
 *
 * <h2>Model</h2>
 * <ol>
 *   <li><b>Water balance</b> — synthetic seasonal rain plus irrigation
 *       multiplied by application efficiency gives water supply. Crop demand
 *       is a constant for the synthetic crop. The deficit ratio drives a
 *       linear yield penalty (FAO-56 Ky).</li>
 *   <li><b>Fertiliser response</b> — Mitscherlich: {@code y = yMax (1 - e^{-kN})}.
 *       Saturating, never negative, never above {@code yMax}.</li>
 *   <li><b>Liebig's law of the minimum</b> — the realised yield is the
 *       minimum of the water-limited and fertiliser-limited yields.</li>
 *   <li><b>Planting-date penalty</b> — plants more than
 *       {@value #PLANTING_GRACE_DAYS} days away from the optimum date incur a
 *       multiplicative loss of {@value #PLANTING_PENALTY_PER_DAY} per day.</li>
 *   <li><b>Economics</b> — cost from fertiliser, irrigation and fixed inputs;
 *       gross margin from yield times price minus cost.</li>
 * </ol>
 *
 * <p>Every constant below is a documented simplification. Values are
 * plausible for Limpopo maize but are not derived from measured data. The
 * architecture note must state this; the UI must show
 * {@link ScenarioResult#NOT_AGRONOMIC_ADVICE}.</p>
 */
@Service
public class ScenarioEngine {

    // -- Water model (Limpopo maize, synthetic) --------------------------
    private static final BigDecimal SEASONAL_CROP_DEMAND_MM = new BigDecimal("600");
    private static final BigDecimal SYNTHETIC_SEASONAL_RAIN_MM = new BigDecimal("150");
    private static final BigDecimal IRRIGATION_EFFICIENCY = new BigDecimal("0.70");
    /** FAO-56 Ky for maize, total season. */
    private static final BigDecimal WATER_STRESS_SENSITIVITY = new BigDecimal("1.25");
    private static final BigDecimal WATER_LIMITED_MAX_THA = new BigDecimal("8.00");

    // -- Fertiliser model ------------------------------------------------
    private static final BigDecimal FERT_MAX_THA = new BigDecimal("8.00");
    private static final BigDecimal FERT_RESPONSE_K = new BigDecimal("0.012");

    // -- Planting-date model ---------------------------------------------
    private static final int OPTIMUM_PLANTING_MONTH = 10;
    private static final int OPTIMUM_PLANTING_DAY = 15;
    private static final int PLANTING_GRACE_DAYS = 14;
    private static final BigDecimal PLANTING_PENALTY_PER_DAY = new BigDecimal("0.005");

    // -- Economics (illustrative synthetic inputs) -----------------------
    private static final BigDecimal FERTILISER_COST_PER_KG = new BigDecimal("12.00");
    private static final BigDecimal IRRIGATION_COST_PER_MM = new BigDecimal("2.50");
    private static final BigDecimal FIXED_COST_PER_HA = new BigDecimal("8500.00");
    private static final BigDecimal MAIZE_PRICE_PER_T = new BigDecimal("3800.00");

    /** mm of water depth → m³ per hectare. */
    private static final BigDecimal MM_TO_M3_PER_HA = new BigDecimal("10");

    /**
     * Compute the result for a scenario, using {@code baseline} for the
     * water-balance reference (currently unused by the model but kept in the
     * signature for future baseline-relative outputs).
     */
    public ScenarioResult run(ScenarioParams params, ScenarioParams baseline) {
        ScenarioParams p = params.requireComplete();

        BigDecimal waterLimited = waterLimitedYield(p.irrigationMm());
        BigDecimal fertiliserLimited = fertiliserLimitedYield(p.fertiliserKgHa());
        BigDecimal rawYield = waterLimited.min(fertiliserLimited);

        BigDecimal plantingFactor = plantingFactor(p.plantingDate());
        BigDecimal yieldTha = rawYield.multiply(plantingFactor)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal waterUseM3Ha = SYNTHETIC_SEASONAL_RAIN_MM.add(p.irrigationMm())
                .multiply(MM_TO_M3_PER_HA)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costPerHa = FERTILISER_COST_PER_KG.multiply(p.fertiliserKgHa())
                .add(IRRIGATION_COST_PER_MM.multiply(p.irrigationMm()))
                .add(FIXED_COST_PER_HA)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossMarginPerHa = yieldTha.multiply(MAIZE_PRICE_PER_T)
                .subtract(costPerHa)
                .setScale(2, RoundingMode.HALF_UP);

        return new ScenarioResult(
                yieldTha,
                waterUseM3Ha,
                costPerHa,
                grossMarginPerHa,
                ScenarioResult.NOT_AGRONOMIC_ADVICE);
    }

    // ------------------------------------------------------------------
    // Model components
    // ------------------------------------------------------------------

    private BigDecimal waterLimitedYield(BigDecimal irrigationMm) {
        BigDecimal effectiveIrrigation = irrigationMm.multiply(IRRIGATION_EFFICIENCY);
        BigDecimal waterSupply = SYNTHETIC_SEASONAL_RAIN_MM.add(effectiveIrrigation);

        BigDecimal deficitRatio;
        if (waterSupply.compareTo(SEASONAL_CROP_DEMAND_MM) >= 0) {
            deficitRatio = BigDecimal.ZERO;
        } else {
            deficitRatio = SEASONAL_CROP_DEMAND_MM.subtract(waterSupply)
                    .divide(SEASONAL_CROP_DEMAND_MM, 6, RoundingMode.HALF_UP);
        }

        BigDecimal stressFactor = BigDecimal.ONE.subtract(
                deficitRatio.multiply(WATER_STRESS_SENSITIVITY));
        if (stressFactor.signum() < 0) {
            stressFactor = BigDecimal.ZERO;
        }
        if (stressFactor.compareTo(BigDecimal.ONE) > 0) {
            stressFactor = BigDecimal.ONE;
        }
        return WATER_LIMITED_MAX_THA.multiply(stressFactor);
    }

    private BigDecimal fertiliserLimitedYield(BigDecimal fertiliserKgHa) {
        double exponent = FERT_RESPONSE_K.multiply(fertiliserKgHa).doubleValue();
        double response = 1.0 - Math.exp(-exponent);
        return FERT_MAX_THA.multiply(BigDecimal.valueOf(response));
    }

    private BigDecimal plantingFactor(LocalDate plantingDate) {
        LocalDate optimum = LocalDate.of(
                plantingDate.getYear(), OPTIMUM_PLANTING_MONTH, OPTIMUM_PLANTING_DAY);
        long days = Math.abs(ChronoUnit.DAYS.between(optimum, plantingDate));
        long penalised = Math.max(0L, days - PLANTING_GRACE_DAYS);
        BigDecimal factor = BigDecimal.ONE.subtract(
                PLANTING_PENALTY_PER_DAY.multiply(BigDecimal.valueOf(penalised)));
        return factor.signum() < 0 ? BigDecimal.ZERO : factor;
    }
}