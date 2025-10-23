#!/bin/bash
# Direct Database Migration Script (No Docker Required)
# Intelligently migrates: Enums, Functions, Policies, Triggers, Indexes, Views, Tables
# Usage: ./migrate-direct.sh [dev-to-staging|staging-to-prod] [--non-interactive]

# NOTE: Not using 'set -e' to handle errors explicitly
# This allows us to capture and display actual error messages

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to read properties from local.properties
read_property() {
    local prop_key=$1
    local prop_value=$(grep "^${prop_key}=" local.properties | cut -d'=' -f2-)
    echo "$prop_value"
}

# Function to extract enum values from SQL
extract_enum_values() {
    local sql_file=$1
    local enum_name=$2

    # Extract the CREATE TYPE statement and parse enum values
    awk "/CREATE TYPE ${enum_name} AS ENUM/,/);/" "$sql_file" | \
        grep -v "CREATE TYPE\|);" | \
        sed "s/[',]//g" | \
        sed 's/^[[:space:]]*//' | \
        grep -v '^$'
}

# Function to generate ALTER TYPE statements for enum changes
generate_enum_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- ENUM TYPE MIGRATIONS (Incremental Updates)" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Find all enum types in source schema
    local enum_types=$(grep "CREATE TYPE.*AS ENUM" "$source_schema" | sed -E 's/CREATE TYPE (.*) AS ENUM.*/\1/')

    local enum_changes_found=false

    for enum_type in $enum_types; do
        # Check if enum exists in target
        if ! grep -q "CREATE TYPE ${enum_type} AS ENUM" "$target_schema"; then
            # New enum - create it
            echo "-- Creating new enum type: $enum_type" >> "$output_file"
            awk "/CREATE TYPE ${enum_type} AS ENUM/,/);/" "$source_schema" >> "$output_file"
            echo "" >> "$output_file"
            enum_changes_found=true
            continue
        fi

        # Extract values from both schemas
        local source_values=$(extract_enum_values "$source_schema" "$enum_type")
        local target_values=$(extract_enum_values "$target_schema" "$enum_type")

        # Find new values (in source but not in target)
        while IFS= read -r value; do
            if [ -n "$value" ] && ! echo "$target_values" | grep -qx "$value"; then
                echo "-- Adding new value '$value' to enum type $enum_type" >> "$output_file"
                echo "ALTER TYPE $enum_type ADD VALUE IF NOT EXISTS '$value';" >> "$output_file"
                enum_changes_found=true
            fi
        done <<< "$source_values"
    done

    if [ "$enum_changes_found" = false ]; then
        echo "-- No enum type changes detected" >> "$output_file"
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

    # Extract all functions from source
    local temp_file=$(mktemp)
    local function_count=0

    # Find function definitions (handle both $$ and $function$ delimiters)
    awk '
        /CREATE OR REPLACE FUNCTION|CREATE FUNCTION/ {
            in_func=1
            func_text=$0"\n"
            next
        }
        in_func {
            func_text=func_text $0"\n"
            if (/\$\$;$/ || /\$[a-zA-Z_][a-zA-Z0-9_]*\$;$/) {
                print func_text
                in_func=0
                func_text=""
            }
        }
    ' "$source_schema" > "$temp_file"

    if [ -s "$temp_file" ]; then
        while IFS= read -r func_def; do
            if [ -n "$func_def" ]; then
                echo "$func_def" >> "$output_file"
                echo "" >> "$output_file"
                ((function_count++))
            fi
        done < "$temp_file"
        echo "-- Migrated $function_count functions" >> "$output_file"
    else
        echo "-- No functions found" >> "$output_file"
    fi

    rm -f "$temp_file"
    echo "" >> "$output_file"
}

# Function to generate policy migrations
generate_policy_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- RLS POLICY MIGRATIONS" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Extract policies from source
    local source_policies=$(grep -E "CREATE POLICY|ALTER TABLE.*ENABLE ROW LEVEL SECURITY" "$source_schema" || echo "")

    if [ -n "$source_policies" ]; then
        # First, enable RLS on tables
        grep "ALTER TABLE.*ENABLE ROW LEVEL SECURITY" "$source_schema" | sort -u >> "$output_file" 2>/dev/null || true
        echo "" >> "$output_file"

        # Then add policies (using DROP + CREATE pattern)
        local policy_names=$(grep "CREATE POLICY" "$source_schema" | sed -E 's/CREATE POLICY "?([^"]*)"? ON.*/\1/' | sort -u)

        for policy in $policy_names; do
            if [ -n "$policy" ]; then
                # Extract table name for this policy
                local table_name=$(grep "CREATE POLICY.*${policy}" "$source_schema" | sed -E 's/.*ON ([^ ]*).*/\1/' | head -1)

                echo "-- Recreating policy: $policy on $table_name" >> "$output_file"
                echo "DROP POLICY IF EXISTS \"$policy\" ON $table_name;" >> "$output_file"

                # Extract full policy definition
                grep -A 5 "CREATE POLICY.*${policy}" "$source_schema" | head -6 >> "$output_file"
                echo "" >> "$output_file"
            fi
        done
    else
        echo "-- No policies found" >> "$output_file"
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

    local triggers=$(grep "CREATE TRIGGER" "$source_schema" | sed -E 's/CREATE TRIGGER ([^ ]*).*/\1/' || echo "")

    if [ -n "$triggers" ]; then
        for trigger in $triggers; do
            if [ -n "$trigger" ]; then
                # Extract table name
                local table_name=$(grep "CREATE TRIGGER ${trigger}" "$source_schema" | sed -E 's/.* ON ([^ ]*).*/\1/' | head -1)

                echo "-- Recreating trigger: $trigger on $table_name" >> "$output_file"
                echo "DROP TRIGGER IF EXISTS $trigger ON $table_name;" >> "$output_file"
                grep -A 2 "CREATE TRIGGER ${trigger}" "$source_schema" | head -3 >> "$output_file"
                echo "" >> "$output_file"
            fi
        done
    else
        echo "-- No triggers found" >> "$output_file"
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

    # Extract CREATE INDEX statements (exclude PKs and unique constraints handled elsewhere)
    local indexes=$(grep "^CREATE.*INDEX" "$source_schema" | grep -v "PRIMARY KEY" || echo "")

    if [ -n "$indexes" ]; then
        echo "$indexes" | while IFS= read -r index_def; do
            if [ -n "$index_def" ]; then
                # Extract index name
                local index_name=$(echo "$index_def" | sed -E 's/CREATE (UNIQUE )?INDEX ([^ ]*).*/\2/')
                echo "-- Creating index: $index_name" >> "$output_file"
                echo "DROP INDEX IF EXISTS $index_name;" >> "$output_file"
                echo "$index_def;" >> "$output_file"
                echo "" >> "$output_file"
            fi
        done
    else
        echo "-- No indexes found" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Function to generate view migrations
generate_view_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- VIEW MIGRATIONS (CREATE OR REPLACE)" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Extract views
    local views=$(awk '/CREATE VIEW|CREATE OR REPLACE VIEW/,/;/' "$source_schema")

    if [ -n "$views" ]; then
        echo "$views" >> "$output_file"
        echo "" >> "$output_file"
    else
        echo "-- No views found" >> "$output_file"
        echo "" >> "$output_file"
    fi
}

# Function to generate table structure migrations
generate_table_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- TABLE STRUCTURE MIGRATIONS" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"
    echo "-- NOTE: This section contains CREATE TABLE statements." >> "$output_file"
    echo "-- These will fail if tables already exist (which is safe)." >> "$output_file"
    echo "-- For column additions/modifications, review and add ALTER TABLE statements manually." >> "$output_file"
    echo "" >> "$output_file"

    # Extract CREATE TABLE statements
    awk '/CREATE TABLE/,/);/' "$source_schema" >> "$output_file"
    echo "" >> "$output_file"
}

# Function to generate extension migrations
generate_extension_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- EXTENSION MIGRATIONS" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Extract extensions from source
    local extensions=$(grep "CREATE EXTENSION" "$source_schema" | sort -u)

    if [ -n "$extensions" ]; then
        echo "$extensions" >> "$output_file"
        echo "" >> "$output_file"
    else
        echo "-- No extensions found" >> "$output_file"
        echo "" >> "$output_file"
    fi
}

# Function to generate sequence migrations
generate_sequence_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- SEQUENCE MIGRATIONS" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"

    # Extract CREATE SEQUENCE statements
    local sequences=$(grep "CREATE SEQUENCE" "$source_schema")

    if [ -n "$sequences" ]; then
        echo "$sequences" | while IFS= read -r seq_def; do
            if [ -n "$seq_def" ]; then
                # Extract sequence name
                local seq_name=$(echo "$seq_def" | sed -E 's/CREATE SEQUENCE ([^ ]*).*/\1/')

                # Check if sequence exists in target
                if ! grep -q "CREATE SEQUENCE ${seq_name}" "$target_schema"; then
                    echo "-- Creating sequence: $seq_name" >> "$output_file"
                    echo "$seq_def;" >> "$output_file"
                    echo "" >> "$output_file"
                fi
            fi
        done
    else
        echo "-- No new sequences found" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Function to generate ALTER TABLE migrations (for review)
generate_alter_table_migrations() {
    local source_schema=$1
    local target_schema=$2
    local output_file=$3

    echo "" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "-- ALTER TABLE STATEMENTS (REVIEW REQUIRED)" >> "$output_file"
    echo "-- ============================================" >> "$output_file"
    echo "" >> "$output_file"
    echo "-- NOTE: These ALTER TABLE statements require manual review." >> "$output_file"
    echo "-- They may modify existing table structures." >> "$output_file"
    echo "-- COMMENTED OUT for safety - uncomment after review." >> "$output_file"
    echo "" >> "$output_file"

    # Extract ALTER TABLE statements (excluding partition attachments which are usually automatic)
    local alter_statements=$(grep "ALTER TABLE" "$source_schema" | grep -v "ATTACH PARTITION" | grep -v "OWNER TO" | grep -v "ENABLE ROW LEVEL SECURITY" | sort -u)

    if [ -n "$alter_statements" ]; then
        echo "$alter_statements" | while IFS= read -r alter_def; do
            if [ -n "$alter_def" ]; then
                echo "-- $alter_def" >> "$output_file"
            fi
        done
        echo "" >> "$output_file"
        echo "-- ⚠️  IMPORTANT: Review and uncomment the ALTER TABLE statements above if needed." >> "$output_file"
    else
        echo "-- No ALTER TABLE statements found" >> "$output_file"
    fi

    echo "" >> "$output_file"
}

# Check if local.properties exists
if [ ! -f "local.properties" ]; then
    echo -e "${RED}❌ local.properties not found!${NC}"
    exit 1
fi

# Check if pg_dump and psql are available
if ! command -v pg_dump &> /dev/null; then
    echo -e "${RED}❌ pg_dump not found!${NC}"
    echo "Please install PostgreSQL client tools:"
    echo "  brew install postgresql"
    exit 1
fi

if ! command -v psql &> /dev/null; then
    echo -e "${RED}❌ psql not found!${NC}"
    echo "Please install PostgreSQL client tools:"
    echo "  brew install postgresql"
    exit 1
fi

# Get migration type
MIGRATION_TYPE=$1
if [ -z "$MIGRATION_TYPE" ]; then
    echo -e "${YELLOW}Usage: $0 [dev-to-staging|staging-to-prod] [--non-interactive]${NC}"
    echo ""
    echo "Examples:"
    echo "  $0 dev-to-staging              # Migrate dev → staging (interactive)"
    echo "  $0 staging-to-prod             # Migrate staging → production (interactive)"
    echo "  $0 dev-to-staging --non-interactive  # Auto-confirm migration (for CI/CD)"
    exit 1
fi

# Check for non-interactive flag
NON_INTERACTIVE=false
if [ "$2" = "--non-interactive" ] || [ "$2" = "-y" ]; then
    NON_INTERACTIVE=true
fi

# Set source and target based on migration type
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
        echo "Use: dev-to-staging or staging-to-prod"
        exit 1
        ;;
esac

echo -e "${BLUE}🚀 Starting $SOURCE_ENV → $TARGET_ENV Migration (Direct DB)${NC}"
echo ""

# Read credentials from local.properties
SOURCE_PROJECT_REF=$(read_property "${SOURCE_ENV}_project_ref")
SOURCE_DB_PASSWORD=$(read_property "${SOURCE_ENV}_db_password")

TARGET_PROJECT_REF=$(read_property "${TARGET_ENV}_project_ref")
TARGET_DB_PASSWORD=$(read_property "${TARGET_ENV}_db_password")

# Validate credentials
if [ -z "$SOURCE_PROJECT_REF" ] || [ -z "$TARGET_PROJECT_REF" ]; then
    echo -e "${RED}❌ Missing project refs in local.properties${NC}"
    exit 1
fi

if [ -z "$SOURCE_DB_PASSWORD" ] || [ -z "$TARGET_DB_PASSWORD" ]; then
    echo -e "${RED}❌ Missing database passwords in local.properties${NC}"
    exit 1
fi

# Build connection strings
SOURCE_CONN="postgresql://postgres:${SOURCE_DB_PASSWORD}@db.${SOURCE_PROJECT_REF}.supabase.co:5432/postgres"
TARGET_CONN="postgresql://postgres:${TARGET_DB_PASSWORD}@db.${TARGET_PROJECT_REF}.supabase.co:5432/postgres"

echo -e "${GREEN}✅ Credentials loaded from local.properties${NC}"
echo "   Source: $SOURCE_ENV ($SOURCE_PROJECT_REF)"
echo "   Target: $TARGET_ENV ($TARGET_PROJECT_REF)"
echo ""

# Create working directory
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
WORK_DIR="supabase/migrations/migration_${SOURCE_ENV}_to_${TARGET_ENV}_${TIMESTAMP}"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

echo -e "${BLUE}📁 Working directory: $WORK_DIR${NC}"
echo ""

# Step 1: Backup Target Database
echo -e "${YELLOW}📦 Step 1: Backing up $TARGET_ENV database...${NC}"
mkdir -p backup
cd backup
BACKUP_FILE="${TARGET_ENV}_backup_${TIMESTAMP}.sql"
BACKUP_ERROR="${TARGET_ENV}_backup_${TIMESTAMP}.log"
echo "Creating full backup (this may take a while)..."

# Try backup with separate error capture
pg_dump "$TARGET_CONN" > $BACKUP_FILE 2> $BACKUP_ERROR
BACKUP_EXIT_CODE=$?

if [ $BACKUP_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}❌ Backup failed!${NC}"
    echo ""
    if [ -s "$BACKUP_ERROR" ]; then
        echo "Error details:"
        cat $BACKUP_ERROR
    fi
    echo ""
    echo "Note: If you see warnings about extensions or permissions, the backup may still be usable."
    echo "Check the backup file size below:"
    ls -lh $BACKUP_FILE 2>/dev/null || echo "Backup file not created"
    cd ../..
    exit 1
fi

BACKUP_SIZE=$(du -h $BACKUP_FILE | cut -f1)
echo -e "${GREEN}✅ Backup created: $BACKUP_FILE ($BACKUP_SIZE)${NC}"

# Show warnings if any (but don't fail)
if [ -s "$BACKUP_ERROR" ]; then
    echo -e "${YELLOW}⚠️  Warnings during backup:${NC}"
    cat $BACKUP_ERROR
    echo ""
fi

BACKUP_PATH="$(pwd)/$BACKUP_FILE"
cd ..

# Step 2: Dump Source Schema
echo ""
echo -e "${YELLOW}🔍 Step 2: Dumping schema from $SOURCE_ENV...${NC}"
mkdir -p source-schema
cd source-schema

SOURCE_SCHEMA_FILE="source_schema_${TIMESTAMP}.sql"
echo "Dumping source schema..."
pg_dump "$SOURCE_CONN" --schema-only --no-owner --no-privileges > $SOURCE_SCHEMA_FILE 2>&1 || {
    echo -e "${RED}❌ Source schema dump failed!${NC}"
    cd ../..
    exit 1
}

SOURCE_SIZE=$(du -h $SOURCE_SCHEMA_FILE | cut -f1)
echo -e "${GREEN}✅ Source schema dumped: $SOURCE_SCHEMA_FILE ($SOURCE_SIZE)${NC}"
cd ..

# Step 3: Dump Target Schema for Comparison
echo ""
echo -e "${YELLOW}📝 Step 3: Dumping current $TARGET_ENV schema for comparison...${NC}"
mkdir -p target-schema
cd target-schema

TARGET_SCHEMA_FILE="target_schema_${TIMESTAMP}.sql"
echo "Dumping target schema..."
pg_dump "$TARGET_CONN" --schema-only --no-owner --no-privileges > $TARGET_SCHEMA_FILE 2>&1 || {
    echo -e "${RED}❌ Target schema dump failed!${NC}"
    cd ../..
    exit 1
}

TARGET_SIZE=$(du -h $TARGET_SCHEMA_FILE | cut -f1)
echo -e "${GREEN}✅ Target schema dumped: $TARGET_SCHEMA_FILE ($TARGET_SIZE)${NC}"
cd ..

# Step 4: Generate Migration (diff between schemas)
echo ""
echo -e "${YELLOW}🔧 Step 4: Generating comprehensive migration...${NC}"
mkdir -p migration
cd migration

MIGRATION_FILE="migration_${SOURCE_ENV}_to_${TARGET_ENV}_${TIMESTAMP}.sql"
ENUM_MIGRATION_FILE="migration_${SOURCE_ENV}_to_${TARGET_ENV}_${TIMESTAMP}_enums.sql"

# Header for enum migration (runs OUTSIDE transaction)
echo "-- ENUM MIGRATIONS (Must run outside transaction)" > $ENUM_MIGRATION_FILE
echo "-- Generated at: $(date)" >> $ENUM_MIGRATION_FILE
echo "-- Source: $SOURCE_ENV ($SOURCE_PROJECT_REF)" >> $ENUM_MIGRATION_FILE
echo "-- Target: $TARGET_ENV ($TARGET_PROJECT_REF)" >> $ENUM_MIGRATION_FILE
echo "" >> $ENUM_MIGRATION_FILE
echo "-- NOTE: ALTER TYPE ADD VALUE cannot run in a transaction block." >> $ENUM_MIGRATION_FILE
echo "-- This file must be applied BEFORE the main migration." >> $ENUM_MIGRATION_FILE
echo "" >> $ENUM_MIGRATION_FILE

echo "  1/7 Analyzing enums..."
generate_enum_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$ENUM_MIGRATION_FILE"

# Header for main migration (runs IN transaction)
echo "-- Comprehensive Migration from $SOURCE_ENV to $TARGET_ENV" > $MIGRATION_FILE
echo "-- Generated at: $(date)" >> $MIGRATION_FILE
echo "-- Source: $SOURCE_ENV ($SOURCE_PROJECT_REF)" >> $MIGRATION_FILE
echo "-- Target: $TARGET_ENV ($TARGET_PROJECT_REF)" >> $MIGRATION_FILE
echo "" >> $MIGRATION_FILE
echo "-- This migration handles:" >> $MIGRATION_FILE
echo "-- 1. Extensions (CREATE EXTENSION)" >> $MIGRATION_FILE
echo "-- 2. Sequences (CREATE SEQUENCE)" >> $MIGRATION_FILE
echo "-- 3. Functions (CREATE OR REPLACE)" >> $MIGRATION_FILE
echo "-- 4. RLS Policies (DROP + CREATE)" >> $MIGRATION_FILE
echo "-- 5. Triggers (DROP + CREATE)" >> $MIGRATION_FILE
echo "-- 6. Indexes (DROP + CREATE)" >> $MIGRATION_FILE
echo "-- 7. Views (CREATE OR REPLACE)" >> $MIGRATION_FILE
echo "-- 8. Tables (CREATE - will fail safely if exists)" >> $MIGRATION_FILE
echo "-- 9. ALTER TABLE (commented - review required)" >> $MIGRATION_FILE
echo "" >> $MIGRATION_FILE
echo "-- NOTE: Enum migrations are in a separate file and applied first." >> $MIGRATION_FILE
echo "" >> $MIGRATION_FILE
echo "BEGIN;" >> $MIGRATION_FILE
echo "" >> $MIGRATION_FILE

echo "  2/10 Analyzing extensions..."
generate_extension_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  3/10 Analyzing sequences..."
generate_sequence_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  4/10 Analyzing functions..."
generate_function_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  5/10 Analyzing policies..."
generate_policy_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  6/10 Analyzing triggers..."
generate_trigger_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  7/10 Analyzing indexes..."
generate_index_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  8/10 Analyzing views..."
generate_view_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  9/10 Analyzing tables..."
generate_table_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "  10/10 Analyzing ALTER TABLE statements..."
generate_alter_table_migrations "../source-schema/$SOURCE_SCHEMA_FILE" "../target-schema/$TARGET_SCHEMA_FILE" "$MIGRATION_FILE"

echo "" >> $MIGRATION_FILE
echo "COMMIT;" >> $MIGRATION_FILE

MIGRATION_SIZE=$(du -h $MIGRATION_FILE | cut -f1)
ENUM_SIZE=$(du -h $ENUM_MIGRATION_FILE | cut -f1)
echo -e "${GREEN}✅ Enum migration generated: $ENUM_MIGRATION_FILE ($ENUM_SIZE)${NC}"
echo -e "${GREEN}✅ Main migration generated: $MIGRATION_FILE ($MIGRATION_SIZE)${NC}"
MIGRATION_PATH="$(pwd)/$MIGRATION_FILE"
ENUM_PATH="$(pwd)/$ENUM_MIGRATION_FILE"

# Step 5: Preview Migration
echo ""
echo -e "${YELLOW}👀 Step 5: Migration Preview${NC}"
echo ""
echo -e "${CYAN}=== ENUM MIGRATIONS (Applied First) ===${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━"
cat $ENUM_MIGRATION_FILE
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━���━━━━━━━━━"
echo ""
echo -e "${CYAN}=== MAIN MIGRATION (First 80 lines) ===${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━"
head -n 80 $MIGRATION_FILE
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${YELLOW}... (showing first 80 lines only) ...${NC}"
echo ""

# Check for breaking changes
if grep -qi "DROP TABLE\|DROP COLUMN\|DROP FUNCTION\|DROP VIEW" $MIGRATION_FILE 2>/dev/null; then
    echo -e "${RED}⚠️  WARNING: Potentially destructive operations detected!${NC}"
    echo "This migration may contain DROP statements."
    echo ""
fi

# Step 6: Analyze differences
echo -e "${YELLOW}📊 Step 6: Schema comparison:${NC}"
echo "Source schema size: $SOURCE_SIZE"
echo "Target schema size: $TARGET_SIZE"
echo "Migration file size: $MIGRATION_SIZE"
echo ""

# Count key objects
SOURCE_TABLES=$(grep -c "CREATE TABLE" ../source-schema/$SOURCE_SCHEMA_FILE || echo "0")
TARGET_TABLES=$(grep -c "CREATE TABLE" ../target-schema/$TARGET_SCHEMA_FILE || echo "0")
echo "Tables in source: $SOURCE_TABLES"
echo "Tables in target: $TARGET_TABLES"
echo ""

SOURCE_FUNCTIONS=$(grep -c "CREATE FUNCTION\|CREATE OR REPLACE FUNCTION" ../source-schema/$SOURCE_SCHEMA_FILE || echo "0")
TARGET_FUNCTIONS=$(grep -c "CREATE FUNCTION\|CREATE OR REPLACE FUNCTION" ../target-schema/$TARGET_SCHEMA_FILE || echo "0")
echo "Functions in source: $SOURCE_FUNCTIONS"
echo "Functions in target: $TARGET_FUNCTIONS"
echo ""

# Step 7: Confirm
if [ "$NON_INTERACTIVE" = "true" ]; then
    echo -e "${GREEN}✅ Non-interactive mode: Automatically applying migration...${NC}"
else
    echo -e "${YELLOW}⚠️  Ready to apply migration to $TARGET_ENV${NC}"
    echo ""
    echo -e "${RED}IMPORTANT: This will DROP and RECREATE schema objects!${NC}"
    echo "Existing data will be preserved, but structure will change."
    echo ""
    read -p "Apply this migration? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo -e "${RED}❌ Migration cancelled${NC}"
        echo ""
        echo -e "${BLUE}📦 Backup saved: $BACKUP_PATH${NC}"
        echo -e "${BLUE}📝 Migration SQL: $MIGRATION_PATH${NC}"
        echo -e "${BLUE}📁 Working directory: $(cd .. && pwd)${NC}"
        cd ..
        exit 0
    fi
fi

# Step 8: Apply Migration
echo ""
echo -e "${YELLOW}🚀 Step 8: Applying migration to $TARGET_ENV...${NC}"
echo "This may take several minutes..."
echo ""

# FIRST: Apply enum migrations (OUTSIDE transaction - required by PostgreSQL)
echo -e "${CYAN}Step 8a: Applying enum migrations (outside transaction)...${NC}"
psql "$TARGET_CONN" -f $ENUM_MIGRATION_FILE || {
    echo -e "${RED}❌ Enum migration failed!${NC}"
    echo ""
    echo -e "${BLUE}📦 Backup is available: $BACKUP_PATH${NC}"
    echo -e "${BLUE}📝 Enum Migration SQL: $ENUM_PATH${NC}"
    echo ""
    echo "To rollback manually:"
    echo "  psql \"$TARGET_CONN\" < $BACKUP_PATH"
    cd ..
    exit 1
}
echo -e "${GREEN}✅ Enum migrations applied successfully!${NC}"
echo ""

# SECOND: Apply main migration (IN transaction)
echo -e "${CYAN}Step 8b: Applying main migration (in transaction)...${NC}"
psql "$TARGET_CONN" -f $MIGRATION_FILE || {
    echo -e "${RED}❌ Main migration failed!${NC}"
    echo ""
    echo -e "${BLUE}📦 Backup is available: $BACKUP_PATH${NC}"
    echo -e "${BLUE}📝 Migration SQL: $MIGRATION_PATH${NC}"
    echo ""
    echo "To rollback manually:"
    echo "  psql \"$TARGET_CONN\" < $BACKUP_PATH"
    cd ..
    exit 1
}

echo -e "${GREEN}✅ All migrations applied successfully!${NC}"

# Step 9: Verify
echo ""
echo -e "${YELLOW}🔍 Step 9: Verifying migration...${NC}"
VERIFY_FILE="verify_${TIMESTAMP}.sql"
pg_dump "$TARGET_CONN" --schema-only --no-owner --no-privileges > $VERIFY_FILE 2>&1

echo "Counting objects after migration..."
VERIFY_TABLES=$(grep -c "CREATE TABLE" $VERIFY_FILE || echo "0")
VERIFY_FUNCTIONS=$(grep -c "CREATE FUNCTION\|CREATE OR REPLACE FUNCTION" $VERIFY_FILE || echo "0")
echo "Tables after migration: $VERIFY_TABLES"
echo "Functions after migration: $VERIFY_FUNCTIONS"

cd ..

# Final Summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━━"
echo -e "${GREEN}🎉 Migration Complete!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━━"
echo ""
echo -e "${BLUE}📦 Backup:${NC}        $BACKUP_PATH"
echo -e "${BLUE}📝 Migration SQL:${NC} $MIGRATION_PATH"
echo -e "${BLUE}📁 Work Directory:${NC} $(pwd)"
echo ""
echo -e "${YELLOW}⚠️  NEXT STEPS:${NC}"
echo "   1. Save the backup file to a secure location"
echo "   2. Test your application in $TARGET_ENV environment"
echo "   3. Run E2E tests to verify everything works"
echo "   4. Clear Apollo cache in the app after migration"
echo "   5. Monitor for any errors"
echo ""
echo -e "${BLUE}To rollback if needed:${NC}"
echo "  psql \"$TARGET_CONN\" < $BACKUP_PATH"
echo ""
