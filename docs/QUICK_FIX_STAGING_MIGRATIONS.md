# Quick Fix: Staging Migration History

## The Problem

Your staging migration failed because:
1. The wrong project ref was being used (prod instead of staging)
2. Migration history is out of sync

## Solution: Run These Commands

### Option 1: Automated Script (Recommended)

```bash
# Run the simplified staging repair script
./scripts/repair-staging-migrations.sh
```

### Option 2: Manual Steps

If the script doesn't work, follow these manual steps:

```bash
# 1. Backup existing migrations
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p "supabase/migrations_backup_${TIMESTAMP}"
cp -r supabase/migrations/* "supabase/migrations_backup_${TIMESTAMP}/" 2>/dev/null || true

# 2. Link to staging (it will read from local.properties)
PROJECT_REF=$(grep "^staging_project_ref=" local.properties | cut -d'=' -f2)
supabase link --project-ref "$PROJECT_REF"

# 3. Clean migrations directory
rm -rf supabase/migrations/*.sql

# 4. Pull fresh schema from staging
supabase db pull

# 5. Review what was pulled
ls -la supabase/migrations/

# 6. If satisfied, commit
git add supabase/migrations/
git commit -m "fix: sync migration history with staging database"
git push
```

## What I Fixed

1. **Script now uses correct property names**: 
   - ✅ `staging_project_ref` (matches your local.properties)
   - ❌ ~~`supabase.project.ref.staging`~~ (was wrong)

2. **Created two scripts**:
   - `scripts/fix-migration-history.sh` - Generic for any environment
   - `scripts/repair-staging-migrations.sh` - Simplified for staging only

## Verify Your Settings

Check that your `local.properties` has these values:

```bash
# Should show: ftnwwiwmljcwzpsawdmf
grep "^staging_project_ref=" local.properties

# Should show: https://ftnwwiwmljcwzpsawdmf.supabase.co
grep "^staging_supabase_url=" local.properties
```

## After Running the Fix

Once migrations are synced:

```bash
# Review the changes
git status
git diff supabase/migrations/

# Commit and push
git add .
git commit -m "fix: resolve staging migration history conflicts"
git push
```

Then re-run your GitHub Actions deployment - it should work now!

