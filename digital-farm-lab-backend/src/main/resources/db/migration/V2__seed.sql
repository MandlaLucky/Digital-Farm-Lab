-- Synthetic seed data only
INSERT INTO users (id, display_name, role) VALUES
 ('11111111-1111-1111-1111-111111111111','Lerato Learner','LEARNER'),
 ('22222222-2222-2222-2222-222222222222','Thabo Trainer','TRAINER')
ON CONFLICT DO NOTHING;

INSERT INTO farms (owner_id, name, province, total_ha) VALUES
 ('11111111-1111-1111-1111-111111111111','Demo Farm','Limpopo', 12.50);

INSERT INTO fields (farm_id, name, area_ha, soil_type, crop_code, planting_date)
SELECT id, 'North Field', 4.20, 'loam', 'MAIZE', DATE '2026-10-15' FROM farms WHERE name='Demo Farm';

INSERT INTO exercises (module_code, title, prompt, rubric, pass_score) VALUES
 ('LEARN-CROP-01','Irrigation scenario', 'Reduce irrigation by 20% and report the yield change.',
  '{"yield_weight":0.6,"reflection_weight":0.4}', 60);
