-- SAFE Enum Table Migration (No Data Loss)
-- This migration converts PostgreSQL ENUMs to tables WITHOUT using CASCADE

-- ============================================
-- STEP 1: Create Enum Tables (alongside existing ENUMs)
-- ============================================

-- Create gender_filter enum table
CREATE TABLE IF NOT EXISTS gender_filter_table (
    value TEXT PRIMARY KEY,
    comment TEXT
);

INSERT INTO gender_filter_table (value) VALUES
    ('MALE'),
    ('FEMALE'),
    ('ANY')
ON CONFLICT (value) DO NOTHING;

-- Create activity_type enum table
CREATE TABLE IF NOT EXISTS activity_type_table (
    value TEXT PRIMARY KEY,
    comment TEXT
);

INSERT INTO activity_type_table (value) VALUES
    ('SESSION'),
    ('CAMP'),
    ('COURSE'),
    ('EVENT'),
    ('CAMPAIGN'),
    ('PROTECTION_SESSION'),
    ('BODH_SESSION')
ON CONFLICT (value) DO NOTHING;

-- Create family_relation enum table
CREATE TABLE IF NOT EXISTS family_relation_table (
    value TEXT PRIMARY KEY,
    comment TEXT
);

INSERT INTO family_relation_table (value) VALUES
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
    ('OTHER')
ON CONFLICT (value) DO NOTHING;

-- ============================================
-- STEP 2: Add New Columns (with _new suffix) that reference the tables
-- ============================================

-- Add new member.gender column (text with FK)
ALTER TABLE member ADD COLUMN IF NOT EXISTS gender_new TEXT REFERENCES gender_filter_table(value);

-- Copy data from old enum column to new text column
UPDATE member
SET gender_new = gender::TEXT
WHERE gender IS NOT NULL AND gender_new IS NULL;

-- Add new activities.type column (text with FK)
ALTER TABLE activities ADD COLUMN IF NOT EXISTS type_new TEXT REFERENCES activity_type_table(value);

-- Copy data from old enum column to new text column
UPDATE activities
SET type_new = type::TEXT
WHERE type IS NOT NULL AND type_new IS NULL;

-- Add new activities.allowed_gender column (text with FK)
ALTER TABLE activities ADD COLUMN IF NOT EXISTS allowed_gender_new TEXT REFERENCES gender_filter_table(value);

-- Copy data from old enum column to new text column
UPDATE activities
SET allowed_gender_new = allowed_gender::TEXT
WHERE allowed_gender IS NOT NULL AND allowed_gender_new IS NULL;

-- Add new family_member.relation_to_head column (text with FK)
ALTER TABLE family_member ADD COLUMN IF NOT EXISTS relation_to_head_new TEXT REFERENCES family_relation_table(value);

-- Copy data from old enum column to new text column
UPDATE family_member
SET relation_to_head_new = relation_to_head::TEXT
WHERE relation_to_head IS NOT NULL AND relation_to_head_new IS NULL;

-- ============================================
-- STEP 3: Verification - Check data was copied correctly
-- ============================================

SELECT 'Verification - Data copied to new columns' as status;

SELECT 'member.gender' as column_name,
       COUNT(*) FILTER (WHERE gender IS NOT NULL) as old_count,
       COUNT(*) FILTER (WHERE gender_new IS NOT NULL) as new_count
FROM member
UNION ALL
SELECT 'activities.type',
       COUNT(*) FILTER (WHERE type IS NOT NULL) as old_count,
       COUNT(*) FILTER (WHERE type_new IS NOT NULL) as new_count
FROM activities
UNION ALL
SELECT 'activities.allowed_gender',
       COUNT(*) FILTER (WHERE allowed_gender IS NOT NULL) as old_count,
       COUNT(*) FILTER (WHERE allowed_gender_new IS NOT NULL) as new_count
FROM activities;

-- ============================================
-- ⚠️  STOP HERE AND VERIFY ⚠️
-- ============================================
-- Before proceeding to drop the old columns, verify that:
-- 1. All enum tables are created
-- 2. All new columns have data
-- 3. old_count == new_count for each column
--
-- If everything looks good, proceed with step4_final.sql

