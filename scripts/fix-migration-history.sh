#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    echo -e "${2}${1}${NC}"
}

print_message "🔧 Migration History Repair Tool" "$BLUE"
echo ""

# Check if environment is provided
if [ -z "$1" ]; then
    print_message "Usage: $0 {dev|staging|prod}" "$RED"
    print_message "Example: $0 staging" "$YELLOW"
    exit 1
fi

ENV=$1

# Load project refs from local.properties
if [ ! -f "local.properties" ]; then
    print_message "❌ local.properties file not found!" "$RED"
    print_message "Please create local.properties with the following format:" "$YELLOW"
    print_message "  dev_project_ref=your-dev-ref" "$NC"
    print_message "  staging_project_ref=your-staging-ref" "$NC"
    print_message "  prod_project_ref=your-prod-ref" "$NC"
    exit 1
fi

# Set project ref based on environment from local.properties
case "$ENV" in
    "dev")
        PROJECT_REF=$(grep "^dev_project_ref=" local.properties | cut -d'=' -f2)
        ;;
    "staging")
        PROJECT_REF=$(grep "^staging_project_ref=" local.properties | cut -d'=' -f2)
        ;;
    "prod")
        PROJECT_REF=$(grep "^prod_project_ref=" local.properties | cut -d'=' -f2)
        ;;
    *)
        print_message "Invalid environment. Use: dev, staging, or prod" "$RED"
        exit 1
        ;;
esac

if [ -z "$PROJECT_REF" ]; then
    print_message "❌ Project ref not found for $ENV environment in local.properties" "$RED"
    print_message "Please add: ${ENV}_project_ref=your-project-ref" "$YELLOW"
    exit 1
fi

print_message "Environment: $ENV" "$BLUE"
print_message "Project Ref: ${PROJECT_REF:0:8}..." "$BLUE"  # Only show first 8 chars
echo ""

# Step 1: Link to the project
print_message "📡 Linking to $ENV project..." "$YELLOW"
supabase link --project-ref $PROJECT_REF

if [ $? -ne 0 ]; then
    print_message "❌ Failed to link project" "$RED"
    exit 1
fi

# Step 2: Backup current migrations
print_message "📦 Backing up current migrations..." "$YELLOW"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="supabase/migrations_backup_${TIMESTAMP}"
mkdir -p "$BACKUP_DIR"
cp -r supabase/migrations/* "$BACKUP_DIR/" 2>/dev/null || true
print_message "✅ Backup created at: $BACKUP_DIR" "$GREEN"

# Step 3: Clean migrations directory
print_message "🧹 Cleaning migrations directory..." "$YELLOW"
rm -rf supabase/migrations/*

# Step 4: Pull fresh schema from remote
print_message "⬇️  Pulling schema from $ENV database..." "$YELLOW"
supabase db pull

if [ $? -ne 0 ]; then
    print_message "❌ Failed to pull schema" "$RED"
    print_message "Restoring backup..." "$YELLOW"
    cp -r "$BACKUP_DIR"/* supabase/migrations/
    exit 1
fi

# Step 5: Check what was pulled
MIGRATION_COUNT=$(ls -1 supabase/migrations/*.sql 2>/dev/null | wc -l)
print_message "✅ Pulled $MIGRATION_COUNT migration(s)" "$GREEN"

# Step 6: Show summary
echo ""
print_message "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" "$BLUE"
print_message "✅ Migration History Synchronized!" "$GREEN"
print_message "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" "$BLUE"
echo ""
print_message "Next steps:" "$YELLOW"
print_message "1. Review the pulled migrations in supabase/migrations/" "$NC"
print_message "2. If satisfied, commit the changes to git" "$NC"
print_message "3. Your backup is available at: $BACKUP_DIR" "$NC"
echo ""
print_message "To apply new migrations, use:" "$YELLOW"
print_message "  supabase db push --include-all" "$GREEN"
echo ""
