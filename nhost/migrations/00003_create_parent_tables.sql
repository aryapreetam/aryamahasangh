-- Migration: Create parent tables (depend on address)
-- Created: 2025-10-24
-- Description: Create arya_samaj and activities tables

-- Table: arya_samaj
CREATE TABLE arya_samaj (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    media_urls TEXT[] NOT NULL DEFAULT '{}',
    address_id TEXT REFERENCES address(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- Table: activities
CREATE TABLE activities (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    type activity_type NOT NULL DEFAULT 'EVENT',
    short_description TEXT NOT NULL,
    long_description TEXT NOT NULL,
    start_datetime TIMESTAMPTZ NOT NULL,
    end_datetime TIMESTAMPTZ NOT NULL,
    media_files TEXT[] NOT NULL DEFAULT '{}',
    additional_instructions TEXT,
    capacity INTEGER DEFAULT 100,
    allowed_gender gender_filter DEFAULT 'ANY',
    overview_description TEXT DEFAULT '',
    overview_media_urls TEXT[] NOT NULL DEFAULT '{}',
    address_id TEXT REFERENCES address(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

