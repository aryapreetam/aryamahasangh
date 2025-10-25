-- Migration: Create base tables (no dependencies)
-- Created: 2025-10-24
-- Description: Create address, organisation, app_labels, and learning tables

-- Table: address
CREATE TABLE address (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    basic_address TEXT,
    state TEXT,
    district TEXT,
    pincode TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    vidhansabha TEXT
);

-- Table: organisation
CREATE TABLE organisation (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    logo TEXT,
    description TEXT NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0
);

-- Table: app_labels
CREATE TABLE app_labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    label_key TEXT,
    label_value TEXT
);

-- Table: learning
CREATE TABLE learning (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT,
    description TEXT,
    url TEXT,
    thumbnail_url TEXT,
    video_id TEXT
);

