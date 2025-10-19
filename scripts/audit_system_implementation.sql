-- ============================================================================
-- USER ACTIVITY LOGGING SYSTEM
-- Supabase Implementation for Arya Mahasangh Platform
-- ============================================================================

-- ============================================================================
-- 1. CORE USER ACTIVITY LOG TABLE
-- ============================================================================

CREATE TABLE user_activity_log
(
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    -- User Attribution (WHO)
    user_id              uuid NOT NULL,
    user_email           text,

    -- Action Details (WHAT)
    action_name          text NOT NULL,
    primary_entity_table text NOT NULL,
    primary_entity_id    uuid NOT NULL,
    affected_tables      text[],

    -- Context & Metadata
    operation_context    text             DEFAULT 'DIRECT_ACTION', -- 'DIRECT_ACTION', 'BULK_OPERATION', 'CASCADING'
    is_bulk              boolean          DEFAULT false,
    record_count         integer          DEFAULT 1,

    -- Data Snapshots (for dashboard details)
    changed_fields       jsonb,                                    -- Only the fields that changed
    metadata             jsonb,                                    -- Additional context

    -- Timing (WHEN)
    performed_at         timestamptz      DEFAULT NOW(),
    transaction_id       bigint           DEFAULT txid_current(),

    -- Constraints
    CONSTRAINT valid_action_name CHECK (action_name ~ '^[A-Z][A-Z0-9_]*$'
) ,
    CONSTRAINT valid_context CHECK (operation_context IN ('DIRECT_ACTION', 'BULK_OPERATION', 'CASCADING'))
);

-- ============================================================================
-- 2. INDEXES FOR DASHBOARD PERFORMANCE
-- ============================================================================

-- Primary dashboard query: "Show me all activities by user"
CREATE INDEX idx_user_activity_user_time ON user_activity_log (user_id, performed_at DESC);

-- Entity-specific queries: "Show me all changes to this organisation"
CREATE INDEX idx_user_activity_entity ON user_activity_log (primary_entity_table, primary_entity_id, performed_at DESC);

-- Action-type queries: "Show me all CREATE_MEMBER actions"
CREATE INDEX idx_user_activity_action ON user_activity_log (action_name, performed_at DESC);

-- Transaction analysis: "Show me all actions in this transaction"
CREATE INDEX idx_user_activity_transaction ON user_activity_log (transaction_id);

-- Performance: Email lookups for dashboard display
CREATE INDEX idx_user_activity_email ON user_activity_log (user_email, performed_at DESC);

-- ============================================================================
-- 3. ROW LEVEL SECURITY FOR USER ACCESS
-- ============================================================================

ALTER TABLE user_activity_log ENABLE ROW LEVEL SECURITY;

-- Users can only view their own activities
CREATE
POLICY user_activity_read_own ON user_activity_log
    FOR
SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- Admin/superuser access (adjust role as needed)
CREATE
POLICY user_activity_admin_full_access ON user_activity_log
    FOR ALL
    TO service_role
    USING (true);

-- No direct INSERT/UPDATE/DELETE for regular users
CREATE
POLICY user_activity_no_modification ON user_activity_log
    FOR INSERT,
UPDATE,
DELETE
    TO authenticated
    USING (false);

-- ============================================================================
-- 4. HELPER FUNCTIONS
-- ============================================================================

-- Extract user info from JWT claims
CREATE
OR REPLACE FUNCTION get_current_user_info()
RETURNS TABLE(user_id uuid, user_email text)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
jwt_claims jsonb;
    extracted_user_id
uuid;
    extracted_email
text;
BEGIN
    -- Get JWT claims
BEGIN
        jwt_claims
:= current_setting('request.jwt.claims', true)::jsonb;
        extracted_user_id
:= (jwt_claims->>'sub')::uuid;
EXCEPTION WHEN OTHERS THEN
        -- No JWT context, return null
        RETURN;
END;
    
    -- Skip if no user context
    IF
extracted_user_id IS NULL THEN
        RETURN;
END IF;
    
    -- Get user email from auth.users
SELECT email
INTO extracted_email
FROM auth.users
WHERE id = extracted_user_id;

-- Return user info
user_id
:= extracted_user_id;
    user_email
:= extracted_email;
    RETURN
NEXT;
END;
$$;

-- Count concurrent operations in current transaction
CREATE
OR REPLACE FUNCTION count_concurrent_operations(table_name text, operation text)
RETURNS integer
LANGUAGE plpgsql
AS $$
DECLARE
op_count integer;
    current_tx
bigint;
BEGIN
    current_tx
:= txid_current();
    
    -- Count operations in current transaction within last 5 seconds
    -- This handles transaction that span multiple seconds
SELECT COUNT(*)
INTO op_count
FROM user_activity_log
WHERE primary_entity_table = table_name
  AND action_name LIKE '%' || operation || '%'
  AND transaction_id = current_tx
  AND performed_at > NOW() - INTERVAL '5 seconds';

RETURN COALESCE(op_count, 0);
END;
$$;

-- ============================================================================
-- 5. SMART USER ACTION DETECTION FUNCTION
-- ============================================================================

CREATE
OR REPLACE FUNCTION log_smart_user_action()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
user_info RECORD;
    action_name
text;
    affected_tables_list
text[];
    concurrent_ops
integer;
    is_bulk_op
boolean := false;
    changed_fields_data
jsonb := '{}';
    record_count_val
integer := 1;
BEGIN
    -- Get current user info
SELECT *
INTO user_info
FROM get_current_user_info();

-- Skip if no user context (system operations)
IF
user_info.user_id IS NULL THEN
        RETURN COALESCE(NEW, OLD);
END IF;
    
    -- Detect bulk operations
    concurrent_ops
:= count_concurrent_operations(TG_TABLE_NAME, TG_OP);
    is_bulk_op
:= concurrent_ops > 2;
    
    -- Smart action name detection
    action_name
:= CASE
        -- ================================================
        -- PRIMARY ENTITY CREATION
        -- ================================================
        WHEN TG_TABLE_NAME = 'organisation' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_CREATE_ORGANISATIONS' ELSE 'CREATE_ORGANISATION'
END
            
        WHEN TG_TABLE_NAME = 'member' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_CREATE_MEMBERS' ELSE 'CREATE_MEMBER'
END
            
        WHEN TG_TABLE_NAME = 'family' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_CREATE_FAMILIES' ELSE 'CREATE_FAMILY'
END
            
        WHEN TG_TABLE_NAME = 'arya_samaj' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_CREATE_ARYA_SAMAJ' ELSE 'CREATE_ARYA_SAMAJ'
END
            
        WHEN TG_TABLE_NAME = 'activities' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_CREATE_ACTIVITIES' ELSE 'CREATE_ACTIVITY'
END
            
        -- ================================================
        -- PRIMARY ENTITY UPDATES
        -- ================================================
        WHEN TG_TABLE_NAME IN ('organisation', 'member', 'family', 'arya_samaj', 'activities') AND TG_OP = 'UPDATE' THEN
            'UPDATE_' || UPPER(TG_TABLE_NAME)
            
        -- ================================================
        -- PRIMARY ENTITY DELETION
        -- ================================================
        WHEN TG_TABLE_NAME IN ('organisation', 'member', 'family', 'arya_samaj', 'activities') AND TG_OP = 'DELETE' THEN
            CASE WHEN is_bulk_op THEN 'BULK_DELETE_' || UPPER(TG_TABLE_NAME) ELSE 'DELETE_' || UPPER(TG_TABLE_NAME)
END
            
        -- ================================================
        -- RELATIONSHIP MANAGEMENT
        -- ================================================
        WHEN TG_TABLE_NAME = 'organisational_member' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_ADD_MEMBERS_TO_ORG' ELSE 'ADD_MEMBER_TO_ORG'
END
            
        WHEN TG_TABLE_NAME = 'organisational_member' AND TG_OP = 'DELETE' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_REMOVE_MEMBERS_FROM_ORG' ELSE 'REMOVE_MEMBER_FROM_ORG'
END
            
        WHEN TG_TABLE_NAME = 'family_member' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_ADD_MEMBERS_TO_FAMILY' ELSE 'ADD_MEMBER_TO_FAMILY'
END
            
        WHEN TG_TABLE_NAME = 'family_member' AND TG_OP = 'DELETE' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_REMOVE_MEMBERS_FROM_FAMILY' ELSE 'REMOVE_MEMBER_FROM_FAMILY'
END
            
        WHEN TG_TABLE_NAME = 'samaj_member' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_ADD_MEMBERS_TO_SAMAJ' ELSE 'ADD_MEMBER_TO_SAMAJ'
END
            
        WHEN TG_TABLE_NAME = 'samaj_member' AND TG_OP = 'DELETE' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_REMOVE_MEMBERS_FROM_SAMAJ' ELSE 'REMOVE_MEMBER_FROM_SAMAJ'
END
            
        WHEN TG_TABLE_NAME = 'activity_member' AND TG_OP = 'INSERT' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_ADD_MEMBERS_TO_ACTIVITY' ELSE 'ADD_MEMBER_TO_ACTIVITY'
END
            
        WHEN TG_TABLE_NAME = 'activity_member' AND TG_OP = 'DELETE' THEN 
            CASE WHEN is_bulk_op THEN 'BULK_REMOVE_MEMBERS_FROM_ACTIVITY' ELSE 'REMOVE_MEMBER_FROM_ACTIVITY'
END
            
        -- ================================================
        -- ADDRESS MANAGEMENT (Context-aware)
        -- ================================================
        WHEN TG_TABLE_NAME = 'address' AND TG_OP = 'UPDATE' THEN 'UPDATE_ADDRESS'
        WHEN TG_TABLE_NAME = 'address' AND TG_OP = 'INSERT' THEN 'CREATE_ADDRESS'
        WHEN TG_TABLE_NAME = 'address' AND TG_OP = 'DELETE' THEN 'DELETE_ADDRESS'
            
        -- ================================================
        -- FALLBACK FOR UNKNOWN OPERATIONS
        -- ================================================
        ELSE TG_TABLE_NAME || '_' || TG_OP
END;
    
    -- Determine affected tables (for multi-table operations)
    affected_tables_list
:= ARRAY[TG_TABLE_NAME];
    
    -- Extract changed fields for UPDATE operations (dashboard details)
    IF
TG_OP = 'UPDATE' THEN
        changed_fields_data := jsonb_build_object(
            'old_values', to_jsonb(OLD),
            'new_values', to_jsonb(NEW),
            'changed_count', (
                SELECT COUNT(*) 
                FROM jsonb_each(to_jsonb(NEW)) new_data
                JOIN jsonb_each(to_jsonb(OLD)) old_data ON new_data.key = old_data.key
                WHERE new_data.value != old_data.value
            )
        );
END IF;
    
    -- Set record count for bulk operations
    IF
is_bulk_op THEN
        record_count_val := concurrent_ops + 1;
END IF;
    
    -- Insert activity log (with deduplication for same transaction)
INSERT INTO user_activity_log (user_id,
                               user_email,
                               action_name,
                               primary_entity_table,
                               primary_entity_id,
                               affected_tables,
                               operation_context,
                               is_bulk,
                               record_count,
                               changed_fields,
                               metadata)
VALUES (user_info.user_id,
        user_info.user_email,
        action_name,
        TG_TABLE_NAME,
        COALESCE(NEW.id, OLD.id),
        affected_tables_list,
        CASE WHEN is_bulk_op THEN 'BULK_OPERATION' ELSE 'DIRECT_ACTION' END,
        is_bulk_op,
        record_count_val,
        changed_fields_data,
        jsonb_build_object(
                'trigger_operation', TG_OP,
                'trigger_table', TG_TABLE_NAME,
                'concurrent_operations', concurrent_ops
        ));

RETURN COALESCE(NEW, OLD);

EXCEPTION WHEN OTHERS THEN
    -- Log the error but don't break the original operation
    INSERT INTO trigger_debug_log (message) VALUES (
        'Activity logging error: ' || SQLERRM || ' for table: ' || TG_TABLE_NAME
    );
RETURN COALESCE(NEW, OLD);
END;
$$;

-- ============================================================================
-- 6. CREATE TRIGGERS FOR ALL TARGET TABLES
-- ============================================================================

-- Primary Business Entity Triggers
CREATE TRIGGER user_activity_member_trigger
    AFTER INSERT OR
UPDATE OR
DELETE
ON member
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

CREATE TRIGGER user_activity_family_trigger
    AFTER INSERT OR
UPDATE OR
DELETE
ON family
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

CREATE TRIGGER user_activity_organisation_trigger
    AFTER INSERT OR
UPDATE OR
DELETE
ON organisation
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

CREATE TRIGGER user_activity_arya_samaj_trigger
    AFTER INSERT OR
UPDATE OR
DELETE
ON arya_samaj
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

CREATE TRIGGER user_activity_activities_trigger
    AFTER INSERT OR
UPDATE OR
DELETE
ON activities
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

-- Relationship Table Triggers
CREATE TRIGGER user_activity_organisational_member_trigger
    AFTER INSERT OR
DELETE
ON organisational_member
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

CREATE TRIGGER user_activity_family_member_trigger
    AFTER INSERT OR
DELETE
ON family_member
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

CREATE TRIGGER user_activity_samaj_member_trigger
    AFTER INSERT OR
DELETE
ON samaj_member
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

CREATE TRIGGER user_activity_activity_member_trigger
    AFTER INSERT OR
DELETE
ON activity_member
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

-- Address Table Trigger (Context-aware)
CREATE TRIGGER user_activity_address_trigger
    AFTER INSERT OR
UPDATE OR
DELETE
ON address
    FOR EACH ROW EXECUTE FUNCTION log_smart_user_action();

-- ============================================================================
-- 7. DASHBOARD QUERY VIEWS
-- ============================================================================

-- Recent Activity Dashboard View
CREATE VIEW dashboard_recent_activities AS
SELECT id,
       user_email,
       action_name,
       primary_entity_table,
       primary_entity_id,
       operation_context,
       is_bulk,
       record_count,
       performed_at,
       CASE
           WHEN is_bulk THEN action_name || ' (' || record_count || ' items)'
           ELSE action_name
           END as display_action
FROM user_activity_log
ORDER BY performed_at DESC;

-- User Activity Summary View
CREATE VIEW dashboard_user_summary AS
SELECT user_id,
       user_email,
       COUNT(*) as total_actions,
       COUNT(*)    FILTER (WHERE is_bulk = true) as bulk_operations, COUNT(*) FILTER (WHERE action_name LIKE 'CREATE_%') as create_operations, COUNT(*) FILTER (WHERE action_name LIKE 'UPDATE_%') as update_operations, COUNT(*) FILTER (WHERE action_name LIKE 'DELETE_%') as delete_operations, MAX(performed_at) as last_activity
FROM user_activity_log
GROUP BY user_id, user_email
ORDER BY last_activity DESC;

-- Entity Change History View
CREATE VIEW dashboard_entity_history AS
SELECT primary_entity_table,
       primary_entity_id,
       user_email,
       action_name,
       operation_context,
       changed_fields,
       performed_at
FROM user_activity_log
WHERE primary_entity_table IN ('member', 'family', 'organisation', 'arya_samaj', 'activities')
ORDER BY primary_entity_table, primary_entity_id, performed_at DESC;

-- ============================================================================
-- 8. MAINTENANCE FUNCTIONS
-- ============================================================================

-- Cleanup old activity logs (run monthly)
CREATE
OR REPLACE FUNCTION cleanup_old_activity_logs(retention_days integer DEFAULT 365)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
deleted_count integer;
BEGIN
DELETE
FROM user_activity_log
WHERE performed_at < NOW() - (retention_days || ' days')::interval;

GET DIAGNOSTICS deleted_count = ROW_COUNT;

-- Log cleanup action
INSERT INTO trigger_debug_log (message)
VALUES ('Cleaned up ' || deleted_count || ' activity log entries older than ' || retention_days || ' days');

RETURN deleted_count;
END;
$$;

-- Performance analysis function
CREATE
OR REPLACE FUNCTION analyze_activity_performance()
RETURNS TABLE(
    metric text,
    value bigint,
    description text
)
LANGUAGE plpgsql
AS $$
BEGIN
RETURN QUERY
SELECT 'total_activities'::text, COUNT(*)::bigint, 'Total activity log entries'::text
FROM user_activity_log

UNION ALL

SELECT 'unique_users'::text, COUNT(DISTINCT user_id)::bigint, 'Unique users with activities'::text
FROM user_activity_log

UNION ALL

SELECT 'bulk_operations'::text, COUNT(*)::bigint, 'Bulk operations performed'::text
FROM user_activity_log
WHERE is_bulk = true

UNION ALL

SELECT 'last_24h_activities'::text, COUNT(*)::bigint, 'Activities in last 24 hours'::text
FROM user_activity_log
WHERE performed_at > NOW() - INTERVAL '24 hours'

UNION ALL

SELECT 'avg_daily_activities'::text, COALESCE(
        COUNT(*) / NULLIF(DATE_PART('day', MAX(performed_at) - MIN(performed_at)), 0),
        0)::bigint, 'Average activities per day'::text
FROM user_activity_log;
END;
$$;

-- ============================================================================
-- 9. DASHBOARD SAMPLE QUERIES
-- ============================================================================

-- Query 1: Recent Activities (WHO, WHAT, WHEN)
/*
SELECT 
    user_email as "कौन",
    display_action as "क्या",
    TO_CHAR(performed_at, 'DD/MM/YYYY HH24:MI') as "कब"
FROM dashboard_recent_activities 
LIMIT 50;
*/

-- Query 2: User Activity Summary
/*
SELECT 
    user_email as "उपयोगकर्ता",
    total_actions as "कुल कार्य",
    create_operations as "निर्माण",
    update_operations as "अद्यतन",
    delete_operations as "हटाना",
    TO_CHAR(last_activity, 'DD/MM/YYYY') as "अंतिम गतिविधि"
FROM dashboard_user_summary;
*/

-- Query 3: Entity Change History
/*
SELECT 
    primary_entity_table as "तालिका",
    primary_entity_id as "आईडी",
    user_email as "उपयोगकर्ता",
    action_name as "कार्य",
    TO_CHAR(performed_at, 'DD/MM/YYYY HH24:MI') as "समय"
FROM dashboard_entity_history
WHERE primary_entity_id = 'YOUR_ENTITY_ID';
*/

-- ============================================================================
-- INSTALLATION COMPLETE
-- ============================================================================

DO
$$
BEGIN
    RAISE
NOTICE 'User Activity Logging System installed successfully!';
    RAISE
NOTICE 'Tables created: user_activity_log';
    RAISE
NOTICE 'Triggers installed on: member, family, organisation, arya_samaj, activities, address, and relationship tables';
    RAISE
NOTICE 'Dashboard views available: dashboard_recent_activities, dashboard_user_summary, dashboard_entity_history';
    RAISE
NOTICE 'Run "SELECT * FROM analyze_activity_performance();" to check system health';
END $$;
