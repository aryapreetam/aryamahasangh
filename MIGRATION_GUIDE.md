# Supabase Migration - Simple Guide

## 🚀 Quick Commands

### Daily Development
```bash
# After making schema changes in Supabase Studio (dev environment)
supabase link --project-ref <your-dev-ref>
supabase db diff -f my_feature_name
git add supabase/migrations/
git commit -m "feat: schema changes"
git push
```

### Migrate Between Environments
```bash
# Dev → Staging
./migrate.sh dev-to-staging

# Staging → Production
./migrate.sh staging-to-prod
```

That's it! The script handles everything:
- ✅ Generates migration comparing schemas
- ✅ Shows preview
- ✅ Checks for breaking changes
- ✅ Creates backup
- ✅ Applies migration

## 🔧 What the CLI Auto-Detects

When you run `supabase db diff`, it automatically finds:
- ✅ New/changed enums
- ✅ New/changed functions
- ✅ New/changed policies
- ✅ New/changed triggers
- ✅ New/changed indexes
- ✅ Table/column changes

**No manual parsing needed!**

## 📚 More Info

- [Official Supabase Docs](https://supabase.com/docs/guides/deployment/database-migrations)
- [Supabase CLI Reference](https://supabase.com/docs/reference/cli/introduction)

