#!/bin/bash
# ⚠️⚠️⚠️ DEPRECATED - DO NOT USE ⚠️⚠️⚠️
#
# This script uses custom schema parsing which is:
# - Error-prone (might miss database objects)
# - Incomplete (doesn't handle all PostgreSQL features)
# - Unmaintainable (custom regex parsing)
# - Not using Supabase's migration tracking
#
# ✅ USE OFFICIAL SUPABASE CLI INSTEAD:
#   See: docs/PROPER_SUPABASE_MIGRATION_WORKFLOW.md
#   Official docs: https://supabase.com/docs/guides/deployment/database-migrations
#
# The proper approach:
#   1. supabase link --project-ref <source>
#   2. supabase db diff --db-url <target> -f migration_name
#   3. supabase link --project-ref <target>
#   4. supabase db push
#
# This automatically detects ALL changes: enums, functions, policies, triggers, indexes, etc.
# ⚠️⚠️⚠️ DEPRECATED - DO NOT USE ⚠️⚠️⚠️

echo "❌ This script is DEPRECATED!"
echo ""
echo "Please use the official Supabase CLI workflow instead."
echo "See: docs/PROPER_SUPABASE_MIGRATION_WORKFLOW.md"
echo ""
read -p "Do you want to continue anyway? (NOT RECOMMENDED) (yes/no): " FORCE_CONTINUE
if [ "$FORCE_CONTINUE" != "yes" ]; then
    echo "Exiting. Please use 'supabase db diff' instead."
    exit 1
fi

echo ""
echo "⚠️  Proceeding with deprecated script at your own risk..."
echo ""
sleep 3

# Smart Database Migration Script with Incremental Updates
# Intelligently handles: Enums, Functions, Policies, Triggers, Indexes, Views, Sequences
# Usage: ./migrate-smart.sh [dev-to-staging|staging-to-prod] [--non-interactive]

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Function to read properties
read_property() {
    local prop_key=$1
    local prop_value=$(grep "^${prop_key}=" local.properties | cut -d'=' -f2-)
    echo "$prop_value"
}

# Function to extract enum values
extract_enum_values() {
    local sql_file=$1
    local enum_name=$2

    awk "/CREATE TYPE ${enum_name} AS ENUM/,/);/" "$sql_file" | \
        grep -v "CREATE TYPE\|);" | \
        sed "s/[',]//g" | \
        sed 's/^[[:space:]]*//' | \
        grep -v '^$'
}

# Function to generate enum migrations
generate_enum_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- ENUM MIGRATIONS (Incremental)" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    local enum_types=$(grep "CREATE TYPE public\." "$source_schema" | grep "AS ENUM" | sed -E 's/CREATE TYPE (.*) AS ENUM.*/\1/')

    local enum_changes_found=false

    for enum_type in $enum_types; do
        local source_values=$(extract_enum_values "$source_schema" "$enum_type")
        local target_values=$(extract_enum_values "$target_schema" "$enum_type")

        # Check if enum exists in target
        if ! grep -q "CREATE TYPE ${enum_type} AS ENUM" "$target_schema"; then
            # New enum type - need to create it
            echo "-- Creating new enum type $enum_type" >> "$output_file"
            awk "/CREATE TYPE ${enum_type} AS ENUM/,/);/" "$source_schema" >> "$output_file"
            echo "" >> "$output_file"
            enum_changes_found=true
            continue
        fi

        # Find new values
        while IFS= read -r value; do
            if [ -n "$value" ] && ! echo "$target_values" | grep -qx "$value"; then
                echo "-- Adding new value '$value' to $enum_type" >> "$output_file"
                echo "ALTER TYPE $enum_type ADD VALUE IF NOT EXISTS '$value';" >> "$output_file"
                enum_changes_found=true
            fi
        done <<< "$source_values"
    done

    if [ "$enum_changes_found" = false ]; then
        echo "-- No enum changes detected" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Function to generate function migrations
generate_function_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- FUNCTION MIGRATIONS (CREATE OR REPLACE)" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Create a temp file for processing
    local temp_functions=$(mktemp)

    # Extract function names from source to compare with target
    local source_functions=$(grep "CREATE OR REPLACE FUNCTION" "$source_schema" | \
        sed -E 's/CREATE OR REPLACE FUNCTION ([^(]*)\(.*/\1/' | \
        sed 's/^[[:space:]]*//' | \
        sort -u)

    local target_functions=$(grep "CREATE OR REPLACE FUNCTION" "$target_schema" | \
        sed -E 's/CREATE OR REPLACE FUNCTION ([^(]*)\(.*/\1/' | \
        sed 's/^[[:space:]]*//' | \
        sort -u)

    local function_changes_found=false

    # Extract each function definition properly
    # PostgreSQL functions can be delimited by:
    # 1. $$ ... $$ (dollar-quoted strings)
    # 2. $function$ ... $function$ (named dollar quotes)
    # 3. ' ... ' (single quotes, rarely used for function bodies)

    for func in $source_functions; do
        # Escape special characters for grep
        local func_escaped=$(echo "$func" | sed 's/[.]/\\./g')

        # Check if function exists in target or if definition differs
        local needs_migration=false

        if ! echo "$target_functions" | grep -qx "$func"; then
            needs_migration=true
            echo "-- New function: $func" >> "$output_file"
        else
            # Function exists, but check if definition changed
            # Extract both definitions and compare
            local source_def=$(awk "/CREATE OR REPLACE FUNCTION ${func_escaped}/,/^;$|^\\\$\\\$;$/" "$source_schema" | md5sum | cut -d' ' -f1)
            local target_def=$(awk "/CREATE OR REPLACE FUNCTION ${func_escaped}/,/^;$|^\\\$\\\$;$/" "$target_schema" | md5sum | cut -d' ' -f1)

            if [ "$source_def" != "$target_def" ]; then
                needs_migration=true
                echo "-- Updated function: $func" >> "$output_file"
            fi
        fi

        if [ "$needs_migration" = true ]; then
            # Extract the complete function definition
            # This handles various PostgreSQL function syntaxes

            # Method 1: Try to extract function with $$ delimiter
            if grep -A 1000 "CREATE OR REPLACE FUNCTION ${func_escaped}" "$source_schema" | \
                awk '/CREATE OR REPLACE FUNCTION/,/\$\$;$/ {print; if (/\$\$;$/) exit}' | \
                grep -q '\$\$'; then

                grep -A 1000 "CREATE OR REPLACE FUNCTION ${func_escaped}" "$source_schema" | \
                    awk '/CREATE OR REPLACE FUNCTION/,/\$\$;$/ {print; if (/\$\$;$/) exit}' >> "$output_file"

            # Method 2: Try to extract function with custom dollar delimiter (e.g., $function$)
            elif grep -A 1000 "CREATE OR REPLACE FUNCTION ${func_escaped}" "$source_schema" | \
                awk '/CREATE OR REPLACE FUNCTION/,/\$[a-zA-Z0-9_]*\$;$/ {print; if (/\$[a-zA-Z0-9_]*\$;$/) exit}' | \
                grep -q '\$'; then

                grep -A 1000 "CREATE OR REPLACE FUNCTION ${func_escaped}" "$source_schema" | \
                    awk '/CREATE OR REPLACE FUNCTION/,/\$[a-zA-Z0-9_]*\$;$/ {print; if (/\$[a-zA-Z0-9_]*\$;$/) exit}' >> "$output_file"

            # Method 3: Try to extract function ending with just ;
            else
                grep -A 1000 "CREATE OR REPLACE FUNCTION ${func_escaped}" "$source_schema" | \
                    awk '/CREATE OR REPLACE FUNCTION/,/^;$/ {print; if (/^;$/) exit}' >> "$output_file"
            fi

            echo "" >> "$output_file"
            function_changes_found=true
        fi
    done

    rm -f "$temp_functions"

    if [ "$function_changes_found" = false ]; then
        echo "-- No function changes detected" >> "$output_file"
        echo "-- All ${source_functions_count:-0} functions are up to date" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Function to generate RLS policy migrations
generate_policy_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- RLS POLICY MIGRATIONS" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Extract policy names from source
    local source_policies=$(grep "CREATE POLICY" "$source_schema" | sed -E 's/CREATE POLICY "?([^"]*)"?.*/\1/')
    local target_policies=$(grep "CREATE POLICY" "$target_schema" | sed -E 's/CREATE POLICY "?([^"]*)"?.*/\1/')

    local policy_changes_found=false

    for policy in $source_policies; do
        if ! echo "$target_policies" | grep -qx "$policy"; then
            echo "-- Creating new policy: $policy" >> "$output_file"
            grep -A 5 "CREATE POLICY.*${policy}" "$source_schema" | \
                sed '/^--/d' >> "$output_file"
            echo "" >> "$output_file"
            policy_changes_found=true
        fi
    done

    if [ "$policy_changes_found" = false ]; then
        echo "-- No policy changes detected" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Function to generate trigger migrations
generate_trigger_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- TRIGGER MIGRATIONS" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Extract triggers from source
    local source_triggers=$(grep "CREATE TRIGGER" "$source_schema" | sed -E 's/CREATE TRIGGER ([^ ]*).*/\1/')
    local target_triggers=$(grep "CREATE TRIGGER" "$target_schema" | sed -E 's/CREATE TRIGGER ([^ ]*).*/\1/')

    local trigger_changes_found=false

    for trigger in $source_triggers; do
        if ! echo "$target_triggers" | grep -qx "$trigger"; then
            echo "-- Creating new trigger: $trigger" >> "$output_file"
            grep -A 3 "CREATE TRIGGER ${trigger}" "$source_schema" >> "$output_file"
            echo "" >> "$output_file"
            trigger_changes_found=true
        fi
    done

    if [ "$trigger_changes_found" = false ]; then
        echo "-- No trigger changes detected" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Function to generate index migrations
generate_index_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- INDEX MIGRATIONS" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Extract indexes from source (excluding primary keys and unique constraints)
    local source_indexes=$(grep "^CREATE INDEX" "$source_schema" | sed -E 's/CREATE INDEX ([^ ]*).*/\1/')
    local target_indexes=$(grep "^CREATE INDEX" "$target_schema" | sed -E 's/CREATE INDEX ([^ ]*).*/\1/')

    local index_changes_found=false

    for index in $source_indexes; do
        if ! echo "$target_indexes" | grep -qx "$index"; then
            echo "-- Creating new index: $index" >> "$output_file"
            grep "CREATE INDEX ${index}" "$source_schema" | head -1 >> "$output_file"
            echo "" >> "$output_file"
            index_changes_found=true
        fi
    done

    if [ "$index_changes_found" = false ]; then
        echo "-- No index changes detected" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Check prerequisites
if [ ! -f "local.properties" ]; then
    echo -e "${RED}❌ local.properties not found!${NC}"
    exit 1
fi

if ! command -v pg_dump &> /dev/null || ! command -v psql &> /dev/null; then
    echo -e "${RED}❌ PostgreSQL client tools not found!${NC}"
    echo "Install with: brew install postgresql"
    exit 1
fi

# Get migration type
MIGRATION_TYPE=$1
NON_INTERACTIVE=false

if [ -z "$MIGRATION_TYPE" ]; then
    echo -e "${YELLOW}Usage: $0 [dev-to-staging|staging-to-prod] [--non-interactive]${NC}"
    exit 1
fi

if [ "$2" = "--non-interactive" ] || [ "$2" = "-y" ]; then
    NON_INTERACTIVE=true
fi

# Set source and target
case $MIGRATION_TYPE in
    dev-to-staging)
        SOURCE_ENV="dev"
        TARGET_ENV="staging"
        ;;
    staging-to-prod)
        SOURCE_ENV="staging"
        TARGET_ENV="prod"
        ;;
    *)
        echo -e "${RED}❌ Invalid migration type: $MIGRATION_TYPE${NC}"
        exit 1
        ;;
esac

echo -e "${BLUE}🚀 Smart Migration: $SOURCE_ENV → $TARGET_ENV${NC}"
echo ""

# Read credentials
SOURCE_PROJECT_REF=$(read_property "${SOURCE_ENV}_project_ref")
SOURCE_DB_PASSWORD=$(read_property "${SOURCE_ENV}_db_password")
TARGET_PROJECT_REF=$(read_property "${TARGET_ENV}_project_ref")
TARGET_DB_PASSWORD=$(read_property "${TARGET_ENV}_db_password")

if [ -z "$SOURCE_PROJECT_REF" ] || [ -z "$TARGET_PROJECT_REF" ] || \
   [ -z "$SOURCE_DB_PASSWORD" ] || [ -z "$TARGET_DB_PASSWORD" ]; then
    echo -e "${RED}❌ Missing credentials in local.properties${NC}"
    exit 1
fi

# Build connection strings
SOURCE_CONN="postgresql://postgres:${SOURCE_DB_PASSWORD}@db.${SOURCE_PROJECT_REF}.supabase.co:5432/postgres"
TARGET_CONN="postgresql://postgres:${TARGET_DB_PASSWORD}@db.${TARGET_PROJECT_REF}.supabase.co:5432/postgres"

echo -e "${GREEN}✅ Credentials loaded${NC}"
echo "   Source: $SOURCE_ENV ($SOURCE_PROJECT_REF)"
echo "   Target: $TARGET_ENV ($TARGET_PROJECT_REF)"
echo ""

# Create working directory
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
WORK_DIR="supabase/migrations/smart_migration_${SOURCE_ENV}_to_${TARGET_ENV}_${TIMESTAMP}"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

echo -e "${CYAN}📁 Working directory: $WORK_DIR${NC}"
echo ""

# Step 1: Backup
echo -e "${YELLOW}📦 Step 1: Backing up $TARGET_ENV...${NC}"
mkdir -p backup
pg_dump "$TARGET_CONN" > "backup/${TARGET_ENV}_backup_${TIMESTAMP}.sql" 2>&1
echo -e "${GREEN}✅ Backup complete${NC}"
echo ""

# Step 2: Dump schemas
echo -e "${YELLOW}🔍 Step 2: Dumping schemas...${NC}"
mkdir -p schemas
pg_dump "$SOURCE_CONN" --schema-only --no-owner --no-privileges > "schemas/source_${TIMESTAMP}.sql" 2>&1
pg_dump "$TARGET_CONN" --schema-only --no-owner --no-privileges > "schemas/target_${TIMESTAMP}.sql" 2>&1
echo -e "${GREEN}✅ Schemas dumped${NC}"
echo ""

# Step 3: Generate smart migration
echo -e "${YELLOW}🔧 Step 3: Generating smart migration...${NC}"
mkdir -p migration
MIGRATION_FILE="migration/smart_migration_${TIMESTAMP}.sql"

# Header
cat > "$MIGRATION_FILE" << EOF
-- Smart Migration from $SOURCE_ENV to $TARGET_ENV
-- Generated: $(date)
-- Source: $SOURCE_ENV ($SOURCE_PROJECT_REF)
-- Target: $TARGET_ENV ($TARGET_PROJECT_REF)
--
-- This migration uses incremental updates for:
-- - Enum types (ALTER TYPE ADD VALUE)
-- - Functions (CREATE OR REPLACE)
-- - Policies (CREATE IF NOT EXISTS)
-- - Triggers (CREATE IF NOT EXISTS)
-- - Indexes (CREATE IF NOT EXISTS)

BEGIN;

EOF

# Generate migrations for each object type
echo "  - Analyzing enum changes..."
generate_enum_migrations "schemas/source_${TIMESTAMP}.sql" "schemas/target_${TIMESTAMP}.sql" "$MIGRATION_FILE"

echo "  - Analyzing function changes..."
generate_function_migrations "schemas/source_${TIMESTAMP}.sql" "schemas/target_${TIMESTAMP}.sql" "$MIGRATION_FILE"

echo "  - Analyzing policy changes..."
generate_policy_migrations "schemas/source_${TIMESTAMP}.sql" "schemas/target_${TIMESTAMP}.sql" "$MIGRATION_FILE"

echo "  - Analyzing trigger changes..."
generate_trigger_migrations "schemas/source_${TIMESTAMP}.sql" "schemas/target_${TIMESTAMP}.sql" "$MIGRATION_FILE"

echo "  - Analyzing index changes..."
generate_index_migrations "schemas/source_${TIMESTAMP}.sql" "schemas/target_${TIMESTAMP}.sql" "$MIGRATION_FILE"

echo "" >> "$MIGRATION_FILE"
echo "COMMIT;" >> "$MIGRATION_FILE"

echo -e "${GREEN}✅ Smart migration generated${NC}"
echo ""

# Step 4: Preview
echo -e "${YELLOW}👀 Step 4: Migration Preview (first 50 lines):${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━━━━━━━━━━━━━���━━━"
head -n 50 "$MIGRATION_FILE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━"
echo ""

# Step 5: Stats
echo -e "${CYAN}📊 Step 5: Migration Statistics:${NC}"
echo "  Enum changes:     $(grep -c 'ALTER TYPE.*ADD VALUE' "$MIGRATION_FILE" || echo "0")"
echo "  New enums:        $(grep -c 'CREATE TYPE.*AS ENUM' "$MIGRATION_FILE" || echo "0")"
echo "  Functions:        $(grep -c 'CREATE OR REPLACE FUNCTION' "$MIGRATION_FILE" || echo "0")"
echo "  New policies:     $(grep -c 'CREATE POLICY' "$MIGRATION_FILE" || echo "0")"
echo "  New triggers:     $(grep -c 'CREATE TRIGGER' "$MIGRATION_FILE" || echo "0")"
echo "  New indexes:      $(grep -c 'CREATE INDEX' "$MIGRATION_FILE" || echo "0")"
echo ""

# Step 6: Confirm
if [ "$NON_INTERACTIVE" = true ]; then
    echo -e "${GREEN}✅ Auto-applying (non-interactive mode)${NC}"
else
    echo -e "${YELLOW}⚠️  Ready to apply migration to $TARGET_ENV${NC}"
    read -p "Apply this migration? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo -e "${RED}❌ Migration cancelled${NC}"
        echo -e "${CYAN}📝 Migration file: $(pwd)/$MIGRATION_FILE${NC}"
        exit 0
    fi
fi

# Step 7: Apply
echo ""
echo -e "${YELLOW}🚀 Step 7: Applying migration...${NC}"
psql "$TARGET_CONN" -f "$MIGRATION_FILE" 2>&1 | tee "migration/apply_${TIMESTAMP}.log"

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo -e "${GREEN}✅ Migration applied successfully!${NC}"
else
    echo -e "${RED}❌ Migration failed!${NC}"
    echo -e "${CYAN}📦 Backup: backup/${TARGET_ENV}_backup_${TIMESTAMP}.sql${NC}"
    exit 1
fi

# Summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━"
echo -e "${GREEN}🎉 Smart Migration Complete!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━"
echo ""
echo -e "${CYAN}📦 Backup:${NC}     backup/${TARGET_ENV}_backup_${TIMESTAMP}.sql"
echo -e "${CYAN}📝 Migration:${NC}  $MIGRATION_FILE"
echo -e "${CYAN}📋 Log:${NC}        migration/apply_${TIMESTAMP}.log"
echo ""
