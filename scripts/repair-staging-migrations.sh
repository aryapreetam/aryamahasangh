#!/bin/bash

# Simple Migration History Repair for Staging
# Run this script to sync your local migrations with staging database

set -e  # Exit on error

echo "🔧 Migration History Repair Tool"
echo ""
echo "Environment: staging"
echo "Project Ref: ftnwwiwm... (from local.properties)"
echo ""

# Step 1: Backup current migrations
echo "📦 Backing up current migrations..."
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="supabase/migrations_backup_${TIMESTAMP}"
mkdir -p "$BACKUP_DIR"
if [ -d "supabase/migrations" ] && [ "$(ls -A supabase/migrations 2>/dev/null)" ]; then
    cp -r supabase/migrations/* "$BACKUP_DIR/" 2>/dev/null || true
    echo "✅ Backup created at: $BACKUP_DIR"
else
    echo "ℹ️  No existing migrations to backup"
fi

# Step 2: Link to staging (read from local.properties)
echo ""
echo "📡 Linking to staging project..."
PROJECT_REF=$(grep "^staging_project_ref=" local.properties | cut -d'=' -f2)
if [ -z "$PROJECT_REF" ]; then
    echo "❌ staging_project_ref not found in local.properties"
    exit 1
fi
supabase link --project-ref "$PROJECT_REF"

# Step 3: Clean migrations directory
echo ""
echo "🧹 Cleaning migrations directory..."
rm -rf supabase/migrations/*.sql 2>/dev/null || true

# Step 4: Pull fresh schema from staging
echo ""
echo "⬇️  Pulling schema from staging database..."
supabase db pull

# Step 5: Check results
echo ""
MIGRATION_COUNT=$(ls -1 supabase/migrations/*.sql 2>/dev/null | wc -l | xargs)
echo "✅ Pulled $MIGRATION_COUNT migration(s)"

# Step 6: Show summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Migration History Synchronized!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Next steps:"
echo "1. Review the pulled migrations in supabase/migrations/"
echo "2. If satisfied, commit the changes to git"
echo "3. Your backup is available at: $BACKUP_DIR"
echo ""
echo "To apply new migrations, use:"
echo "  supabase db push --include-all"
echo ""

