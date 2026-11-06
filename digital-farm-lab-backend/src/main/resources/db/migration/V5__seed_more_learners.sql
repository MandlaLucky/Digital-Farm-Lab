-- V5: seed three more learners with a complete farm -> field -> scenario -> run
-- -> submission chain so the trainer report shows real diversity, not one row.

-- ── 1. Learners ─────────────────────────────────────────────────────────
INSERT INTO users (id, display_name, role) VALUES
    ('33333333-3333-3333-3333-333333333333', 'Sipho Dlamini',  'LEARNER'),
    ('44444444-4444-4444-4444-444444444444', 'Nomsa Khumalo',  'LEARNER'),
    ('55555555-5555-5555-5555-555555555555', 'Thabo Mokoena',  'LEARNER')
ON CONFLICT (id) DO NOTHING;

-- ── 2. Farms ────────────────────────────────────────────────────────────
INSERT INTO farms (id, owner_id, name, province, total_ha) VALUES
    ('a3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'Dlamini Farm',  'Mpumalanga',      8.50),
    ('a4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'Khumalo Plot',  'KwaZulu-Natal',   5.00),
    ('a5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'Mokoena Field', 'Limpopo',        12.00)
ON CONFLICT (id) DO NOTHING;

-- ── 3. Fields ───────────────────────────────────────────────────────────
INSERT INTO fields (id, farm_id, name, area_ha, soil_type, crop_code, planting_date) VALUES
    ('b3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 'North Plot', 3.00, 'loam',  'MAIZE', DATE '2026-10-15'),
    ('b4444444-4444-4444-4444-444444444444', 'a4444444-4444-4444-4444-444444444444', 'Garden Plot',2.00, 'clay',  'MAIZE', DATE '2026-10-15'),
    ('b5555555-5555-5555-5555-555555555555', 'a5555555-5555-5555-5555-555555555555', 'East Field', 4.00, 'sandy', 'MAIZE', DATE '2026-10-15')
ON CONFLICT (id) DO NOTHING;

-- ── 4. Scenarios ────────────────────────────────────────────────────────
INSERT INTO scenarios (id, field_id, created_by, name, params) VALUES
    ('c3333333-3333-3333-3333-333333333333', 'b3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333',
     'Sipho irrigation trial',
     '{"irrigationMm":550,"fertiliserKgHa":180,"plantingDate":"2026-10-15","irrigationReductionPct":0}'::jsonb),
    ('c4444444-4444-4444-4444-444444444444', 'b4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444',
     'Nomsa 20% trial',
     '{"irrigationMm":440,"fertiliserKgHa":180,"plantingDate":"2026-10-15","irrigationReductionPct":20}'::jsonb),
    ('c5555555-5555-5555-5555-555555555555', 'b5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555',
     'Thabo first attempt',
     '{"irrigationMm":440,"fertiliserKgHa":180,"plantingDate":"2026-10-15","irrigationReductionPct":20}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- ── 5. Runs ─────────────────────────────────────────────────────────────
-- Sipho overrides scenario params (550 -> 440): params_differ = TRUE, score 70
INSERT INTO runs (id, scenario_id, run_by, params, results, baseline) VALUES
    ('d3333333-3333-3333-3333-333333333333',
     'c3333333-3333-3333-3333-333333333333',
     '33333333-3333-3333-3333-333333333333',
     '{"irrigationMm":440,"fertiliserKgHa":180,"plantingDate":"2026-10-15","irrigationReductionPct":20}'::jsonb,
     '{"yieldTha":5.63,"waterUseM3Ha":5900,"costPerHa":11760,"grossMarginPerHa":9634,"disclaimer":"Simulated teaching model. NOT agronomic advice."}'::jsonb,
     '{"yieldTha":6.92,"waterUseM3Ha":7000,"costPerHa":12035,"grossMarginPerHa":14261,"disclaimer":"Simulated teaching model. NOT agronomic advice."}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- Nomsa uses scenario params as-is: params_differ = FALSE, score 60
INSERT INTO runs (id, scenario_id, run_by, params, results, baseline) VALUES
    ('d4444444-4444-4444-4444-444444444444',
     'c4444444-4444-4444-4444-444444444444',
     '44444444-4444-4444-4444-444444444444',
     '{"irrigationMm":440,"fertiliserKgHa":180,"plantingDate":"2026-10-15","irrigationReductionPct":20}'::jsonb,
     '{"yieldTha":5.63,"waterUseM3Ha":5900,"costPerHa":11760,"grossMarginPerHa":9634,"disclaimer":"Simulated teaching model. NOT agronomic advice."}'::jsonb,
     '{"yieldTha":6.92,"waterUseM3Ha":7000,"costPerHa":12035,"grossMarginPerHa":14261,"disclaimer":"Simulated teaching model. NOT agronomic advice."}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- Thabo uses scenario params as-is too: params_differ = FALSE, score 40
INSERT INTO runs (id, scenario_id, run_by, params, results, baseline) VALUES
    ('d5555555-5555-5555-5555-555555555555',
     'c5555555-5555-5555-5555-555555555555',
     '55555555-5555-5555-5555-555555555555',
     '{"irrigationMm":440,"fertiliserKgHa":180,"plantingDate":"2026-10-15","irrigationReductionPct":20}'::jsonb,
     '{"yieldTha":5.63,"waterUseM3Ha":5900,"costPerHa":11760,"grossMarginPerHa":9634,"disclaimer":"Simulated teaching model. NOT agronomic advice."}'::jsonb,
     '{"yieldTha":6.92,"waterUseM3Ha":7000,"costPerHa":12035,"grossMarginPerHa":14261,"disclaimer":"Simulated teaching model. NOT agronomic advice."}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- ── 6. Submissions ──────────────────────────────────────────────────────
-- Scores reflect what the current rubric would award against each run.
-- Sipho: params_differ (10) + irrigation_target (40) + reflection (20) = 70
-- Nomsa: irrigation_target (40) + reflection (20) = 60
-- Thabo: irrigation_target (40) only = 40

INSERT INTO submissions (exercise_id, run_id, user_id, score, reflection, created_at)
SELECT
    e.id,
    'd3333333-3333-3333-3333-333333333333'::uuid,
    '33333333-3333-3333-3333-333333333333'::uuid,
    70,
    'By reducing irrigation by twenty percent I expected the yield to drop, and the engine confirmed a drop of roughly eighteen percent. The deficit stress factor stayed within the linear zone of the FAO-56 curve, which is exactly what we discussed in class. Next time I would test a fifteen percent reduction to see where the threshold is.',
    now() - interval '3 days'
FROM exercises e WHERE e.module_code = 'LEARN-CROP-01';

INSERT INTO submissions (exercise_id, run_id, user_id, score, reflection, created_at)
SELECT
    e.id,
    'd4444444-4444-4444-4444-444444444444'::uuid,
    '44444444-4444-4444-4444-444444444444'::uuid,
    60,
    'Reducing irrigation by twenty percent lowered the yield compared with the baseline in a linear way. The soil moisture content is a key factor here, and I can see why deficit stress directly affects the crop yield in the model. I would like to compare this with a lower irrigation level in a future run.',
    now() - interval '2 days'
FROM exercises e WHERE e.module_code = 'LEARN-CROP-01';

INSERT INTO submissions (exercise_id, run_id, user_id, score, reflection, created_at)
SELECT
    e.id,
    'd5555555-5555-5555-5555-555555555555'::uuid,
    '55555555-5555-5555-5555-555555555555'::uuid,
    40,
    'I reduced irrigation and the yield went down. The model works.',
    now() - interval '1 day'
FROM exercises e WHERE e.module_code = 'LEARN-CROP-01';