INSERT INTO sensor_readings (field_id, type, value, recorded_at)
SELECT
    f.id,
    t.type,
    CASE t.type
        WHEN 'SOIL_MOISTURE' THEN (40 + (sin(d::float / 10) * 10))::numeric(10,3)
        WHEN 'RAIN_MM'       THEN GREATEST(0, (sin(d::float / 5) * 15 + 5)::numeric(10,3))
        WHEN 'TEMP_C'        THEN (22 + (sin(d::float / 15) * 8))::numeric(10,3)
    END,
    (now()::date - d)::timestamptz + INTERVAL '8 hours'
FROM fields f
CROSS JOIN (VALUES ('SOIL_MOISTURE'), ('RAIN_MM'), ('TEMP_C')) AS t(type)
CROSS JOIN generate_series(0, 89) AS d
ON CONFLICT (field_id, type, recorded_at) DO NOTHING;