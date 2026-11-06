-- V7: add metadata JSONB columns so the frontend's rich shapes can round-trip.
-- The core engine columns stay as-is; metadata stores presentation extras
-- (fields, water sources, weather, learning objectives, per-stage series, etc).
ALTER TABLE farms     ADD COLUMN IF NOT EXISTS metadata JSONB;
ALTER TABLE scenarios ADD COLUMN IF NOT EXISTS metadata JSONB;
ALTER TABLE runs      ADD COLUMN IF NOT EXISTS metadata JSONB;