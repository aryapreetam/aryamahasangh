#!/bin/bash

# Commit all migration fixes and trigger GitHub Actions
# This script ensures all changes are properly committed and pushed

set -e

echo "🔍 Checking modified files..."
echo ""

# Show what files are modified
echo "Modified files:"
git status --short

echo ""
echo "📦 Staging all changes..."

# Add all modified files
git add .gitignore
git add .github/workflows/db-migration.yml
git add local.properties.template
git add scripts/
git add docs/

echo ""
echo "📋 Files staged for commit:"
git status --short

echo ""
echo "✍️  Creating commit..."

# Create comprehensive commit message
git commit -m "fix: resolve migration history conflicts and update CI/CD workflow

This commit fixes the staging migration failure with the following changes:

## GitHub Actions Workflow
- Remove deprecated --linked flag from supabase db diff
- Fix migration push to create proper migration files first
- Add --include-all flag for out-of-order migrations

## Migration Repair Scripts
- Add fix-migration-history.sh for all environments
- Add repair-staging-migrations.sh for quick staging fix
- Load project refs securely from local.properties
- No hardcoded secrets

## Configuration
- Update local.properties.template with staging configuration
- Remove /docs/ and /scripts/ from .gitignore
- Ensure scripts and docs are version controlled

## Documentation
- Add MIGRATION_HISTORY_FIX.md with comprehensive guide
- Add QUICK_FIX_STAGING_MIGRATIONS.md for quick reference

Fixes the following errors:
- ERROR: unknown flag: --linked
- Migration history out of sync between local and remote
- Duplicate key violations in schema_migrations table

This will enable successful staging deployments via GitHub Actions."

echo ""
echo "✅ Commit created successfully!"
echo ""
echo "🚀 Pushing to remote repository..."

# Get current branch
CURRENT_BRANCH=$(git branch --show-current)

# Push to remote
git push origin "$CURRENT_BRANCH"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ All changes pushed successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🎯 Next steps:"
echo "1. GitHub Actions will trigger on push (workflow path changed)"
echo "2. Run ./scripts/repair-staging-migrations.sh to fix local migration history"
echo "3. Monitor GitHub Actions at: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\(.*\)\.git/\1/')/actions"
echo ""

