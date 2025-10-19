-- 📊 Complete GraphQL Configuration for Supabase
-- This script ensures ALL tables and views have proper GraphQL configuration
-- Run this after database migration to enable complete GraphQL API

-- ===============================================
-- SCHEMA LEVEL CONFIGURATION
-- ===============================================

-- Enable camelCase field naming globally
COMMENT ON SCHEMA public IS '@graphql({"inflect_names": true})';

-- ===============================================
-- TABLE LEVEL CONFIGURATION
-- ===============================================

-- Core Business Tables with totalCount enabled
COMMENT ON TABLE public.organisation IS '@graphql({"totalCount": {"enabled": true}})';
COMMENT ON TABLE public.member IS '@graphql({"totalCount": {"enabled": true}})';
COMMENT ON TABLE public.family IS '@graphql({"totalCount": {"enabled": true}})';
COMMENT ON TABLE public.family_member IS '@graphql({"totalCount": {"enabled": true}})';
COMMENT ON TABLE public.activities IS '@graphql({"totalCount": {"enabled": true}})';
COMMENT ON TABLE public.arya_samaj IS '@graphql({"totalCount": {"enabled": true}})';

-- Registration and Learning Tables
COMMENT ON TABLE public.satr_registration IS '@graphql({"totalCount": {"enabled": true}})';
COMMENT ON TABLE public.learning IS '@graphql({"totalCount": {"enabled": true}})';
COMMENT ON TABLE public.organisational_member IS '@graphql({"totalCount": {"enabled": true}})';

-- Address Tables (only if they exist in your schema)
COMMENT ON TABLE public.address IS '@graphql({"totalCount": {"enabled": false}})';

-- System and Audit Tables
COMMENT ON TABLE public.audit_log IS '@graphql({"totalCount": {"enabled": true}})';

-- ===============================================
-- DYNAMIC TABLE CONFIGURATION
-- ===============================================

-- Use this query to generate GraphQL comments for ALL your actual tables
-- Copy the output and execute it to configure all existing tables

SELECT 'COMMENT ON TABLE public.' || tablename ||
       ' IS ''@graphql({"totalCount": {"enabled": ' ||
       CASE
           WHEN tablename IN
                ('organisation', 'member', 'family', 'family_member', 'activities', 'arya_samaj', 'satr_registration',
                 'learning', 'organisational_member', 'audit_log')
               THEN 'true'
           ELSE 'false'
           END ||
       '}});''' as graphql_comment_sql
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename NOT LIKE 'pg_%'
  AND tablename NOT LIKE 'information_schema%'
ORDER BY tablename;

-- ===============================================
-- VIEW LEVEL CONFIGURATION
-- ===============================================

-- Views require explicit primary key column definitions
COMMENT ON VIEW public.activities_with_status IS '@graphql({
  "primary_key_columns": ["id"],
  "totalCount": {"enabled": true}
})';

COMMENT ON VIEW public.arya_samaj_with_address IS '@graphql({
  "primary_key_columns": ["id"], 
  "totalCount": {"enabled": true}
})';

COMMENT ON VIEW public.member_in_organisation IS '@graphql({
  "primary_key_columns": ["id"],
  "totalCount": {"enabled": true}
})';

COMMENT ON VIEW public.member_not_in_family IS '@graphql({
  "primary_key_columns": ["id"],
  "totalCount": {"enabled": true}
})';

COMMENT ON VIEW public.family_with_member_count IS '@graphql({
  "primary_key_columns": ["id"],
  "totalCount": {"enabled": true}
})';

COMMENT ON VIEW public.organisation_with_member_count IS '@graphql({
  "primary_key_columns": ["id"],
  "totalCount": {"enabled": true}
})';

-- ===============================================
-- DYNAMIC VIEW CONFIGURATION
-- ===============================================

-- Use this query to generate GraphQL comments for ALL your actual views
-- Copy the output and execute it to configure all existing views

SELECT 'COMMENT ON VIEW public.' || viewname ||
       ' IS ''@graphql({' ||
       '  "primary_key_columns": ["id"],' ||
       '  "totalCount": {"enabled": true}' ||
       '});''' as graphql_view_comment_sql
FROM pg_views
WHERE schemaname = 'public'
ORDER BY viewname;

-- ===============================================
-- VERIFICATION QUERIES
-- ===============================================

-- Query to see ALL your actual tables (for reference)
SELECT 'TABLE'                            as object_type,
       tablename                          as object_name,
       obj_description(c.oid, 'pg_class') as current_graphql_comment
FROM pg_tables t
         LEFT JOIN pg_class c ON c.relname = t.tablename
WHERE t.schemaname = 'public'
UNION ALL
SELECT 'VIEW'                             as object_type,
       viewname                           as object_name,
       obj_description(c.oid, 'pg_class') as current_graphql_comment
FROM pg_views v
         LEFT JOIN pg_class c ON c.relname = v.viewname
WHERE v.schemaname = 'public'
ORDER BY object_type, object_name;

-- ===============================================
-- USAGE INSTRUCTIONS
-- ===============================================

/*
STEP-BY-STEP USAGE:

1. First, see what tables and views you actually have:
   Run the "VERIFICATION QUERIES" section above

2. Generate dynamic configuration for your actual schema:
   a) Run the "DYNAMIC TABLE CONFIGURATION" query
   b) Copy the generated COMMENT statements and execute them
   c) Run the "DYNAMIC VIEW CONFIGURATION" query  
   d) Copy the generated COMMENT statements and execute them

3. Apply the configuration:
   - Via Supabase CLI: supabase db remote --linked exec --file configure-graphql-complete.sql
   - Via Dashboard: Copy/paste the generated statements into SQL Editor

4. Verify everything worked:
   Run the verification queries again to see the applied comments

CUSTOMIZATION:
- Edit the CASE statement in DYNAMIC TABLE CONFIGURATION to control which tables get totalCount enabled
- Modify the view configuration template if your views don't use 'id' as primary key
- Add any custom GraphQL configurations for specific functions

This approach ensures you only configure tables and views that actually exist in your database!
*/
