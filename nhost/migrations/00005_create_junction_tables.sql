-- Migration: Create junction/relationship tables
-- Created: 2025-10-24
-- Description: Create activity_member, family_member, organisational_member, organisational_activity, and samaj_member tables

-- Table: activity_member
CREATE TABLE activity_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id TEXT NOT NULL REFERENCES activities(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    member_id TEXT NOT NULL REFERENCES member(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    post TEXT,
    priority SMALLINT NOT NULL
);

-- Table: family_member
CREATE TABLE family_member (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    family_id TEXT NOT NULL REFERENCES family(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    member_id TEXT NOT NULL REFERENCES member(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    is_head BOOLEAN NOT NULL DEFAULT false,
    relation_to_head family_relation NOT NULL
);

-- Table: organisational_member
CREATE TABLE organisational_member (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id TEXT NOT NULL REFERENCES organisation(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    member_id TEXT NOT NULL REFERENCES member(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    post TEXT,
    priority SMALLINT NOT NULL,
    PRIMARY KEY (id, organisation_id, member_id)
);

-- Table: organisational_activity
CREATE TABLE organisational_activity (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id TEXT NOT NULL REFERENCES activities(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    organisation_id TEXT NOT NULL REFERENCES organisation(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- Table: samaj_member
CREATE TABLE samaj_member (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    member_id TEXT REFERENCES member(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    arya_samaj_id TEXT REFERENCES arya_samaj(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    post TEXT,
    priority SMALLINT
);

