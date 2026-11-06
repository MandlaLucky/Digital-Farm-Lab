package com.mlm.digitalfarm.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.entity.Submission;
import com.mlm.digitalfarm.dto.scenario.ScenarioParams;
import com.mlm.digitalfarm.dto.scenario.RunSnapshot;

/**
 * Deterministic rubric scorer. Pure with respect to the submission — it
 * reads the run snapshot, applies the rubric, and returns a score.
 *
 * <p>Supported criterion types (see {@link Exercise} javadoc for the JSON shape):</p>
 * <ul>
 *   <li>{@code PARAMS_DIFFER} — run params differ from scenario params.</li>
 *   <li>{@code PARAMS_RANGE} — a specific run params field is within {@code [min, max]}.</li>
 *   <li>{@code RESULT_RANGE} — a specific results field is within {@code [min, max]}.</li>
 *   <li>{@code RESULT_MAX_PCT_DELTA_FROM_BASELINE} — {@code |run - baseline| / |baseline| <= maxPctDelta}.</li>
 *   <li>{@code REFLECTION_MIN_WORDS} — reflection has at least {@code minWords} words.</li>
 * </ul>
 *
 * <p>The final score is normalised to 0-100 (weights do not have to sum to
 * 100). Any criterion that throws while evaluating is treated as
 * <strong>not satisfied</strong> — a rubric bug must never inflate a score.</p>
 */
@Service
public class ScoringService {

    public ScoringOutcome score(Map<String, Object> rubric,
                                int passScore,
                                RunSnapshot run,
                                String reflection) {
        List<Map<String, Object>> criteria = asListOfMaps(rubric.get("criteria"));

        int totalWeight = 0;
        int earnedWeight = 0;
        List<CriterionResult> breakdown = new ArrayList<>(criteria.size());

        for (Map<String, Object> criterion : criteria) {
            int weight = asInt(criterion.get("weight"), 0);
            String key = asString(criterion.get("key"));
            String label = asString(criterion.getOrDefault("label", key));

            boolean satisfied;
            try {
                satisfied = evaluate(criterion, run, reflection);
            } catch (RuntimeException ex) {
                // Defensive: a malformed rubric row must not crash a submission.
                satisfied = false;
            }

            int earned = satisfied ? weight : 0;
            totalWeight += weight;
            earnedWeight += earned;
            breakdown.add(new CriterionResult(key, label, weight, earned, satisfied));
        }

        int score = totalWeight == 0
                ? 0
                : (int) Math.round(100.0 * earnedWeight / totalWeight);

        return new ScoringOutcome(score, score >= passScore, passScore, breakdown);
    }

    // ------------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------------

    private boolean evaluate(Map<String, Object> criterion, RunSnapshot run, String reflection) {
        String type = asString(criterion.get("type"));
        return switch (type) {
            case "PARAMS_DIFFER" -> !Objects.equals(run.runParams(), run.scenarioParams());
            case "PARAMS_RANGE" -> inRange(criterion, run.runParams());
            case "RESULT_RANGE" -> inRange(criterion, run.results());
            case "RESULT_MAX_PCT_DELTA_FROM_BASELINE" ->
                    maxPctDeltaFromBaseline(criterion, run.results(), run.baseline());
            case "REFLECTION_MIN_WORDS" -> reflectionMinWords(criterion, reflection);
            default -> false;
        };
    }

    private boolean inRange(Map<String, Object> criterion, Map<String, Object> data) {
        Double value = asDouble(data.get(asString(criterion.get("field"))));
        Double min = asDouble(criterion.get("min"));
        Double max = asDouble(criterion.get("max"));
        if (value == null || (min == null && max == null)) {
            return false;
        }
        if (min != null && value < min) return false;
        if (max != null && value > max) return false;
        return true;
    }

    private boolean maxPctDeltaFromBaseline(Map<String, Object> criterion,
                                            Map<String, Object> results,
                                            Map<String, Object> baseline) {
        String field = asString(criterion.get("field"));
        Double actual = asDouble(results.get(field));
        Double base = asDouble(baseline.get(field));
        Double maxPctDelta = asDouble(criterion.get("maxPctDelta"));
        if (actual == null || base == null || maxPctDelta == null || base == 0d) {
            return false;
        }
        double delta = Math.abs(actual - base) / Math.abs(base);
        return delta <= maxPctDelta;
    }

    private boolean reflectionMinWords(Map<String, Object> criterion, String reflection) {
        if (reflection == null || reflection.isBlank()) {
            return false;
        }
        int minWords = asInt(criterion.get("minWords"), 0);
        long wordCount = Arrays.stream(reflection.trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .count();
        return wordCount >= minWords;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private int asInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof CharSequence text) {
            try {
                return Double.parseDouble(text.toString());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Result types
    // ------------------------------------------------------------------

    public record ScoringOutcome(int score, boolean passed, int passScore,
                                 List<CriterionResult> breakdown) { }

    public record CriterionResult(String key, String label,
                                  int weight, int earned, boolean satisfied) { }
}