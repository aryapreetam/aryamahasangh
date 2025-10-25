-- Migration: Create member and family tables
-- Created: 2025-10-24
-- Description: Create member and family tables that depend on address and arya_samaj

-- Table: member
CREATE TABLE member (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    profile_image TEXT,
    educational_qualification TEXT,
    phone_number TEXT NOT NULL DEFAULT '',
    email TEXT,
    dob DATE,
    address_id TEXT REFERENCES address(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    arya_samaj_id TEXT REFERENCES arya_samaj(id) ON DELETE SET NULL ON UPDATE NO ACTION,
    joining_date DATE,
    temp_address_id TEXT REFERENCES address(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    referrer_id TEXT REFERENCES member(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    occupation TEXT,
    introduction TEXT,
    gender gender_filter,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Table: family
CREATE TABLE family (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    address_id TEXT REFERENCES address(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    arya_samaj_id TEXT REFERENCES arya_samaj(id) ON DELETE SET NULL ON UPDATE NO ACTION,
    name TEXT,
    photos TEXT[],
    updated_at TIMESTAMPTZ DEFAULT now()
);

