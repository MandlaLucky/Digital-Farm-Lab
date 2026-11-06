CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('LEARNER','TRAINER','ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE farms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    province VARCHAR(60) NOT NULL,
    total_ha NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id UUID NOT NULL REFERENCES farms(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    area_ha NUMERIC(10,2) NOT NULL,
    soil_type VARCHAR(40) NOT NULL,
    crop_code VARCHAR(40) NOT NULL,
    planting_date DATE NOT NULL
);

CREATE TABLE sensor_readings (
    id BIGSERIAL PRIMARY KEY,
    field_id UUID NOT NULL REFERENCES fields(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL CHECK (type IN ('SOIL_MOISTURE','RAIN_MM','TEMP_C')),
    value NUMERIC(10,3) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    UNIQUE (field_id, type, recorded_at)
);

CREATE TABLE scenarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_id UUID NOT NULL REFERENCES fields(id) ON DELETE CASCADE,
    created_by UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    params JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    run_by UUID NOT NULL,
    params JSONB NOT NULL,
    results JSONB NOT NULL,
    baseline JSONB NOT NULL,
    score INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_code VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    prompt TEXT NOT NULL,
    rubric JSONB NOT NULL,
    pass_score INT NOT NULL DEFAULT 60
);

CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    run_id UUID NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    score INT NOT NULL,
    reflection TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fields_farm        ON fields(farm_id);
CREATE INDEX idx_scenarios_field    ON scenarios(field_id);
CREATE INDEX idx_runs_scenario      ON runs(scenario_id);
CREATE INDEX idx_submissions_ex      ON submissions(exercise_id);
CREATE INDEX idx_sensor_readings    ON sensor_readings(field_id, type, recorded_at DESC);
