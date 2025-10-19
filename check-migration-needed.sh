#!/bin/bash
# Check if database migration is needed
# Compares schemas between source and target databases
# Usage: ./check-migration-needed.sh [dev-to-staging|staging-to-prod]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to read properties from local.properties
read_property() {
    local prop_key=$1
    local prop_value=$(grep "^${prop_key}=" local.properties | cut -d'=' -f2-)
    echo "$prop_value"
}

# Get migration type
MIGRATION_TYPE=$1
if [ -z "$MIGRATION_TYPE" ]; then
    echo -e "${RED}Usage: $0 [dev-to-staging|staging-to-prod]${NC}"
    exit 1
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
        echo -e "${RED}Invalid migration type: $MIGRATION_TYPE${NC}"
        exit 1
        ;;
esac

echo -e "${BLUE}🔍 Checking if migration is needed: $SOURCE_ENV → $TARGET_ENV${NC}"

# Read credentials
SOURCE_PROJECT_REF=$(read_property "${SOURCE_ENV}_project_ref")
SOURCE_DB_PASSWORD=$(read_property "${SOURCE_ENV}_db_password")
TARGET_PROJECT_REF=$(read_property "${TARGET_ENV}_project_ref")
TARGET_DB_PASSWORD=$(read_property "${TARGET_ENV}_db_password")

if [ -z "$SOURCE_PROJECT_REF" ] || [ -z "$TARGET_PROJECT_REF" ]; then
    echo -e "${RED}Missing credentials in local.properties${NC}"
    exit 1
fi

# Build connection strings
SOURCE_CONN="postgresql://postgres:${SOURCE_DB_PASSWORD}@db.${SOURCE_PROJECT_REF}.supabase.co:5432/postgres"
TARGET_CONN="postgresql://postgres:${TARGET_DB_PASSWORD}@db.${TARGET_PROJECT_REF}.supabase.co:5432/postgres"

# Create temp directory
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

echo "Dumping source schema..."
pg_dump "$SOURCE_CONN" --schema-only --no-owner --no-privileges > "$TEMP_DIR/source.sql" 2>/dev/null || {
    echo -e "${RED}Failed to dump source schema${NC}"
    exit 1
}

echo "Dumping target schema..."
pg_dump "$TARGET_CONN" --schema-only --no-owner --no-privileges > "$TEMP_DIR/target.sql" 2>/dev/null || {
    echo -e "${RED}Failed to dump target schema${NC}"
    exit 1
}

# Normalize schemas (remove comments, empty lines, version-specific text)
normalize_schema() {
    grep -v "^--" "$1" | \
    grep -v "^$" | \
    sed 's/PostgreSQL [0-9.]*//g' | \
    sort
}

normalize_schema "$TEMP_DIR/source.sql" > "$TEMP_DIR/source_normalized.sql"
normalize_schema "$TEMP_DIR/target.sql" > "$TEMP_DIR/target_normalized.sql"

# Compare schemas
if diff -q "$TEMP_DIR/source_normalized.sql" "$TEMP_DIR/target_normalized.sql" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Schemas are identical - NO MIGRATION NEEDED${NC}"
    echo "migration_needed=false"
    exit 0
else
    echo -e "${YELLOW}⚠️  Schemas differ - MIGRATION NEEDED${NC}"
    echo ""
    echo "Differences found:"
    diff "$TEMP_DIR/source_normalized.sql" "$TEMP_DIR/target_normalized.sql" | head -20
    echo ""
    echo "migration_needed=true"
    exit 0
fi

