-- Convert PostgreSQL ENUMs to Enum Tables for Hasura GraphQL Support
-- This migration creates enum tables and migrates existing data

-- ============================================
-- STEP 0: Drop Old PostgreSQL ENUM Types First
-- ============================================
-- These need to be dropped before we can create tables with the same names

DROP TYPE IF EXISTS gender_filter CASCADE;
DROP TYPE IF EXISTS activity_type CASCADE;
DROP TYPE IF EXISTS family_relation CASCADE;

-- ============================================
-- STEP 1: Create Enum Tables
-- ============================================

-- Create gender_filter enum table
CREATE TABLE gender_filter (
    value TEXT PRIMARY KEY,
    comment TEXT
);

INSERT INTO gender_filter (value) VALUES
    ('MALE'),
    ('FEMALE'),
    ('ANY');

-- Create activity_type enum table
CREATE TABLE activity_type (
    value TEXT PRIMARY KEY,
    comment TEXT
);

INSERT INTO activity_type (value) VALUES
    ('SESSION'),
    ('CAMP'),
    ('COURSE'),
    ('EVENT'),
    ('CAMPAIGN'),
    ('PROTECTION_SESSION'),
    ('BODH_SESSION');

-- Create family_relation enum table
CREATE TABLE family_relation (
    value TEXT PRIMARY KEY,
    comment TEXT
);

INSERT INTO family_relation (value) VALUES
    ('SELF'),
    ('FATHER'),
    ('MOTHER'),
    ('HUSBAND'),
    ('WIFE'),
    ('SON'),
    ('DAUGHTER'),
    ('BROTHER'),
    ('SISTER'),
    ('GRANDFATHER'),
    ('GRANDMOTHER'),
    ('GRANDSON'),
    ('GRANDDAUGHTER'),
    ('UNCLE'),
    ('AUNT'),
    ('COUSIN'),
    ('NEPHEW'),
    ('NIECE'),
    ('GUARDIAN'),
    ('RELATIVE'),
    ('OTHER');

-- ============================================
-- STEP 2: Migrate member.gender
-- ============================================

-- Add new column with foreign key
ALTER TABLE member
ADD COLUMN gender_new TEXT REFERENCES gender_filter(value);

-- Copy data from old enum column to new text column
UPDATE member
SET gender_new = gender::TEXT
WHERE gender IS NOT NULL;

-- Drop old enum column
ALTER TABLE member DROP COLUMN gender;

-- Rename new column to original name
ALTER TABLE member RENAME COLUMN gender_new TO gender;

-- ============================================
-- STEP 3: Migrate activities.type
-- ============================================

-- Add new column with foreign key
ALTER TABLE activities
ADD COLUMN type_new TEXT REFERENCES activity_type(value);

-- Copy data from old enum column to new text column
UPDATE activities
SET type_new = type::TEXT
WHERE type IS NOT NULL;

-- Drop old enum column
ALTER TABLE activities DROP COLUMN type;

-- Rename new column to original name
ALTER TABLE activities RENAME COLUMN type_new TO type;

-- ============================================
-- STEP 4: Migrate activities.allowed_gender
-- ============================================

-- Add new column with foreign key
ALTER TABLE activities
ADD COLUMN allowed_gender_new TEXT REFERENCES gender_filter(value);

-- Copy data from old enum column to new text column
UPDATE activities
SET allowed_gender_new = allowed_gender::TEXT
WHERE allowed_gender IS NOT NULL;

-- Drop old enum column
ALTER TABLE activities DROP COLUMN allowed_gender;

-- Rename new column to original name
ALTER TABLE activities RENAME COLUMN allowed_gender_new TO allowed_gender;

-- ============================================
-- STEP 5: Migrate family_member.relation_to_head
-- ============================================

-- Add new column with foreign key
ALTER TABLE family_member
ADD COLUMN relation_to_head_new TEXT REFERENCES family_relation(value);

-- Copy data from old enum column to new text column
UPDATE family_member
SET relation_to_head_new = relation_to_head::TEXT
WHERE relation_to_head IS NOT NULL;

-- Drop old enum column
ALTER TABLE family_member DROP COLUMN relation_to_head;

-- Rename new column to original name
ALTER TABLE family_member RENAME COLUMN relation_to_head_new TO relation_to_head;

-- ============================================
-- STEP 6: Verification
-- ============================================

-- Verify enum tables
SELECT 'gender_filter' as enum_table, COUNT(*) as value_count FROM gender_filter
UNION ALL
SELECT 'activity_type', COUNT(*) FROM activity_type
UNION ALL
SELECT 'family_relation', COUNT(*) FROM family_relation;

-- Verify data migration
SELECT 'member.gender' as column_name, COUNT(*) as migrated_count
FROM member WHERE gender IS NOT NULL
UNION ALL
SELECT 'activities.type', COUNT(*)
FROM activities WHERE type IS NOT NULL
UNION ALL
SELECT 'activities.allowed_gender', COUNT(*)
FROM activities WHERE allowed_gender IS NOT NULL
UNION ALL
SELECT 'family_member.relation_to_head', COUNT(*)
FROM family_member WHERE relation_to_head IS NOT NULL;

