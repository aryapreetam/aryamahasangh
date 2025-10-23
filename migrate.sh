#!/bin/bash
# Supabase Migration Helper - The Simple Way
# Usage: ./migrate.sh [dev-to-staging|staging-to-prod]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Check if Supabase CLI is installed
if ! command -v supabase &> /dev/null; then
    echo -e "${RED}❌ Supabase CLI not installed!${NC}"
    echo "Install with: brew install supabase/tap/supabase"
    exit 1
fi

# Read environment config
read_property() {
    local prop_key=$1
    grep "^${prop_key}=" local.properties 2>/dev/null | cut -d'=' -f2-
}

# Parse arguments
MIGRATION_TYPE=$1

if [ -z "$MIGRATION_TYPE" ]; then
    echo -e "${YELLOW}Usage: $0 [dev-to-staging|staging-to-prod]${NC}"
    exit 1
fi

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
        echo "Use: dev-to-staging OR staging-to-prod"
        exit 1
        ;;
esac

echo -e "${CYAN}🚀 Migration: $SOURCE_ENV → $TARGET_ENV${NC}"
echo ""

# Read credentials from local.properties
SOURCE_REF=$(read_property "${SOURCE_ENV}_project_ref")
SOURCE_PASSWORD=$(read_property "${SOURCE_ENV}_db_password")
TARGET_REF=$(read_property "${TARGET_ENV}_project_ref")
TARGET_PASSWORD=$(read_property "${TARGET_ENV}_db_password")

if [ -z "$SOURCE_REF" ] || [ -z "$TARGET_REF" ] || [ -z "$SOURCE_PASSWORD" ] || [ -z "$TARGET_PASSWORD" ]; then
    echo -e "${RED}❌ Missing credentials in local.properties${NC}"
    echo "Need: ${SOURCE_ENV}_project_ref, ${SOURCE_ENV}_db_password, ${TARGET_ENV}_project_ref, ${TARGET_ENV}_db_password"
    exit 1
fi

echo -e "${GREEN}✅ Loaded credentials${NC}"
echo "   Source: $SOURCE_ENV ($SOURCE_REF)"
echo "   Target: $TARGET_ENV ($TARGET_REF)"
echo ""

# Step 1: Link to source
echo -e "${YELLOW}📡 Linking to $SOURCE_ENV...${NC}"
supabase link --project-ref "$SOURCE_REF"
echo ""

# Step 2: Generate migration
TIMESTAMP=$(date +%Y%m%d%H%M%S)
MIGRATION_NAME="${SOURCE_ENV}_to_${TARGET_ENV}_${TIMESTAMP}"
TARGET_DB_URL="postgresql://postgres:${TARGET_PASSWORD}@db.${TARGET_REF}.supabase.co:5432/postgres"

echo -e "${YELLOW}🔍 Generating migration (comparing schemas)...${NC}"
supabase db diff \
  --db-url "$TARGET_DB_URL" \
  --schema public \
  -f "$MIGRATION_NAME"

MIGRATION_FILE=$(ls -t supabase/migrations/*.sql | head -1)

if [ ! -f "$MIGRATION_FILE" ]; then
    echo -e "${RED}❌ Migration file not generated!${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Migration generated: $MIGRATION_FILE${NC}"
echo ""

# Step 3: Show preview
echo -e "${CYAN}📋 Migration Preview:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
head -n 30 "$MIGRATION_FILE"
echo "..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Step 4: Check for breaking changes
echo -e "${YELLOW}🔍 Checking for breaking changes...${NC}"
if grep -qE "DROP TABLE|DROP COLUMN|ALTER TYPE.*DROP" "$MIGRATION_FILE"; then
    echo -e "${RED}⚠️  WARNING: Breaking changes detected!${NC}"
    grep -E "DROP TABLE|DROP COLUMN|ALTER TYPE.*DROP" "$MIGRATION_FILE" | head -5
    echo ""
fi

# Step 5: Confirm
echo -e "${YELLOW}Ready to apply to $TARGET_ENV?${NC}"
read -p "Continue? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo -e "${RED}❌ Cancelled${NC}"
    echo -e "${CYAN}Migration saved: $MIGRATION_FILE${NC}"
    exit 0
fi

# Step 6: Backup (optional but recommended)
echo ""
echo -e "${YELLOW}💾 Creating backup of $TARGET_ENV...${NC}"
mkdir -p backups
BACKUP_FILE="backups/${TARGET_ENV}_backup_${TIMESTAMP}.sql"
pg_dump "$TARGET_DB_URL" > "$BACKUP_FILE" 2>&1 || echo "⚠️  Backup failed (continuing anyway)"
echo -e "${GREEN}✅ Backup saved: $BACKUP_FILE${NC}"
echo ""

# Step 7: Link to target and apply
echo -e "${YELLOW}🚀 Applying migration to $TARGET_ENV...${NC}"
supabase link --project-ref "$TARGET_REF"
supabase db push

if [ $? -eq 0 ]; then
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${GREEN}🎉 Migration Complete!${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo -e "${CYAN}📦 Backup:${NC}    $BACKUP_FILE"
    echo -e "${CYAN}📝 Migration:${NC} $MIGRATION_FILE"
else
    echo ""
    echo -e "${RED}❌ Migration failed!${NC}"
    echo -e "${CYAN}Restore backup: psql \"$TARGET_DB_URL\" < $BACKUP_FILE${NC}"
    exit 1
fi

