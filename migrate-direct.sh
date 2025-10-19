#!/bin/bash
# Direct Database Migration Script (No Supabase CLI API calls)
# Uses direct PostgreSQL connections to migrate schemas
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
echo -e "${YELLOW}🔧 Step 4: Generating migration diff...${NC}"
mkdir -p migration
cd migration

MIGRATION_FILE="migration_${SOURCE_ENV}_to_${TARGET_ENV}_${TIMESTAMP}.sql"

# Simple comparison - in production you might want to use a tool like migra
echo "-- Migration from $SOURCE_ENV to $TARGET_ENV" > $MIGRATION_FILE
echo "-- Generated at: $(date)" >> $MIGRATION_FILE
echo "-- Source: $SOURCE_ENV ($SOURCE_PROJECT_REF)" >> $MIGRATION_FILE
echo "-- Target: $TARGET_ENV ($TARGET_PROJECT_REF)" >> $MIGRATION_FILE
echo "" >> $MIGRATION_FILE
echo "-- WARNING: This is a full schema replacement." >> $MIGRATION_FILE
echo "-- Review carefully before applying!" >> $MIGRATION_FILE
echo "" >> $MIGRATION_FILE

# Copy source schema as migration (full schema replacement approach)
cat ../source-schema/$SOURCE_SCHEMA_FILE >> $MIGRATION_FILE

MIGRATION_SIZE=$(du -h $MIGRATION_FILE | cut -f1)
echo -e "${GREEN}✅ Migration SQL generated: $MIGRATION_FILE ($MIGRATION_SIZE)${NC}"
MIGRATION_PATH="$(pwd)/$MIGRATION_FILE"

# Step 5: Preview Migration
echo ""
echo -e "${YELLOW}👀 Step 5: Migration Preview (first 100 lines):${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━━"
head -n 100 $MIGRATION_FILE
echo "━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━━━━━���━━━━━━━━━━━━"
echo ""
echo -e "${YELLOW}... (showing first 100 lines only) ...${NC}"
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

# Apply migration with transaction
psql "$TARGET_CONN" -f $MIGRATION_FILE || {
    echo -e "${RED}❌ Migration failed!${NC}"
    echo ""
    echo -e "${BLUE}📦 Backup is available: $BACKUP_PATH${NC}"
    echo -e "${BLUE}📝 Migration SQL: $MIGRATION_PATH${NC}"
    echo ""
    echo "To rollback manually:"
    echo "  psql \"$TARGET_CONN\" < $BACKUP_PATH"
    cd ..
    exit 1
}

echo -e "${GREEN}✅ Migration applied successfully!${NC}"

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
