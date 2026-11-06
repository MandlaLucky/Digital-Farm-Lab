-- V6: recompute scores for submissions that were stored under the legacy
-- rubric shape {"yield_weight":0.6,"reflection_weight":0.4}, which the
-- current ScoringService cannot parse (missing "criteria" array -> score 0).
--
-- No dependency on V4 or any timestamp lookup -- just recompute every
-- submission whose stored score is 0 against the current rubric criteria.
-- Idempotent: for submissions that legitimately score 0, the recompute
-- also yields 0, so no false positives.

UPDATE submissions s
SET score = (
    CASE WHEN r.params <> sc.params THEN 10 ELSE 0 END
  + CASE WHEN (r.params->>'irrigationReductionPct')::numeric BETWEEN 15 AND 25
         THEN 40 ELSE 0 END
  + CASE WHEN ABS((r.results->>'yieldTha')::numeric - (r.baseline->>'yieldTha')::numeric)
         / NULLIF((r.baseline->>'yieldTha')::numeric, 0) <= 0.10
         THEN 30 ELSE 0 END
  + CASE WHEN LENGTH(COALESCE(s.reflection, '')) > 0
              AND array_length(string_to_array(trim(s.reflection), ' '), 1) >= 30
         THEN 20 ELSE 0 END
)
FROM runs r
JOIN scenarios sc ON sc.id = r.scenario_id
WHERE s.run_id = r.id
  AND s.score = 0;

-- Diagnostic: report how many rows ended up with score > 0
DO $$
DECLARE
    fixed_count INTEGER;
    total_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_count FROM submissions;
    SELECT COUNT(*) INTO fixed_count FROM submissions WHERE score > 0;
    RAISE NOTICE 'V6: total submissions = %, with score > 0 = %', total_count, fixed_count;
END $$;