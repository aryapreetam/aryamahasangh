-- SAFE Enum Table Migration - Part 2 (FINAL STEP)
-- Only run this AFTER verifying Part 1 completed successfully

-- ============================================
-- STEP 4: Drop Old Enum Columns (SAFE - data is already in new columns)
-- ============================================

-- Drop old member.gender column
ALTER TABLE member DROP COLUMN IF EXISTS gender;

-- Drop old activities.type column
ALTER TABLE activities DROP COLUMN IF EXISTS type;

-- Drop old activities.allowed_gender column
ALTER TABLE activities DROP COLUMN IF EXISTS allowed_gender;

-- Drop old family_member.relation_to_head column
ALTER TABLE family_member DROP COLUMN IF EXISTS relation_to_head;

-- ============================================
-- STEP 5: Rename New Columns to Original Names
-- ============================================

-- Rename member.gender_new to gender
ALTER TABLE member RENAME COLUMN gender_new TO gender;

-- Rename activities.type_new to type
ALTER TABLE activities RENAME COLUMN type_new TO type;

-- Rename activities.allowed_gender_new to allowed_gender
ALTER TABLE activities RENAME COLUMN allowed_gender_new TO allowed_gender;

-- Rename family_member.relation_to_head_new to relation_to_head
ALTER TABLE family_member RENAME COLUMN relation_to_head_new TO relation_to_head;

-- ============================================
-- STEP 6: Drop Old PostgreSQL ENUM Types FIRST (Now safe - no columns depend on them)
-- ============================================

DROP TYPE IF EXISTS gender_filter CASCADE;
DROP TYPE IF EXISTS activity_type CASCADE;
DROP TYPE IF EXISTS family_relation CASCADE;

-- ============================================
-- STEP 7: Rename Enum Tables (remove _table suffix) - Now safe, no name conflicts
-- ============================================

-- Rename gender_filter_table to gender_filter
ALTER TABLE gender_filter_table RENAME TO gender_filter;

-- Rename activity_type_table to activity_type
ALTER TABLE activity_type_table RENAME TO activity_type;

-- Rename family_relation_table to family_relation
ALTER TABLE family_relation_table RENAME TO family_relation;

-- ============================================
-- STEP 8: Final Verification
-- ============================================

SELECT 'Final Verification' as status;

-- Check enum tables exist and have values
SELECT 'gender_filter' as enum_table, COUNT(*) as value_count FROM gender_filter
UNION ALL
SELECT 'activity_type', COUNT(*) FROM activity_type
UNION ALL
SELECT 'family_relation', COUNT(*) FROM family_relation;

-- Check columns have data
SELECT 'member.gender' as column_name, COUNT(*) as record_count
FROM member WHERE gender IS NOT NULL
UNION ALL
SELECT 'activities.type', COUNT(*)
FROM activities WHERE type IS NOT NULL
UNION ALL
SELECT 'activities.allowed_gender', COUNT(*)
FROM activities WHERE allowed_gender IS NOT NULL;

-- Check foreign key constraints exist
SELECT
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public'
    AND ccu.table_name IN ('gender_filter', 'activity_type', 'family_relation')
ORDER BY tc.table_name, kcu.column_name;

SELECT '✅ Migration Complete!' as status;
SELECT 'Next: Track enum tables in Hasura Console for GraphQL enum support' as next_step;

