# Storage Migration Guide: Supabase → Nhost

## Overview
This guide will help you migrate all 104 referenced files from Supabase Storage to Nhost Storage.

## Prerequisites

### 1. Install Python Dependencies
```bash
cd /Users/preetam/workspace/AryaMahasangh
pip3 install -r scripts/storage_migration_requirements.txt
```

### 2. Set Environment Variables

You need to set the following environment variables before running the migration:

```bash
# Supabase credentials (for downloading files)
export SUPABASE_ANON_KEY="your_supabase_anon_key"
# OR
export SUPABASE_SERVICE_ROLE_KEY="your_supabase_service_role_key"

# Nhost credentials (for uploading files)
export NHOST_ADMIN_SECRET="your_nhost_admin_secret"
export NHOST_SUBDOMAIN="hwdbpplmrdjdhcsmdleh"
```

### 3. Create Nhost Storage Buckets

Before running the migration, create these buckets in Nhost console (https://app.nhost.io/):

1. **documents** bucket
   - Permission: Public read
   - Description: Main documents bucket for profiles and activity media

2. **profile_image** bucket
   - Permission: Public read
   - Description: Member profile images

To create buckets:
1. Go to Nhost Console → Your Project
2. Navigate to Storage section
3. Click "Create Bucket"
4. Set name and permissions
5. Save

## Running the Migration

### Step 1: Run the Migration Script

```bash
cd /Users/preetam/workspace/AryaMahasangh
python3 scripts/migrate_storage_to_nhost.py
```

The script will:
1. ✅ Load the file inventory (104 files)
2. ✅ Create temporary download directory
3. ✅ Download all files from Supabase Storage
4. ✅ Upload all files to Nhost Storage
5. ✅ Generate SQL file to update database URLs
6. ✅ Create migration log with detailed results

### Step 2: Update Database URLs

After successful migration, run the generated SQL file:

```bash
# The script generates: nhost/migrations/00012_update_storage_urls.sql
```

You can apply it using:
```bash
# Option A: Using Supabase prod MCP tools
# (The SQL file will have the exact commands)

# Option B: Manual review and execution
# Open the SQL file and review the UPDATE statements
```

### Step 3: Verify Migration

1. Check Nhost Storage console to verify all files are uploaded
2. Test file access by visiting a few URLs in browser
3. Run your application and verify images load correctly

## File Structure

### Files to Migrate:
- **41 profile images** from `member.profile_image`
- **53 activity media files** from `activities.media_files`
- **10 activity overview images** from `activities.overview_media_urls`

### Buckets:
1. `documents` (72 files)
   - Profile images: profile_*.jpg, profile_*.webp
   - Activity media: activity_*.webp, numbered *.jpg
   - Subfolder: `activity_overview/` (10 files)

2. `profile_image` (22 files)
   - Member profiles: acharya_*.webp, arya_*.webp, etc.

### Skipped Files (Invalid):
- 3 book_orders entries with local file paths (not in Supabase Storage)

## Troubleshooting

### Issue: "SUPABASE_ANON_KEY not set"
**Solution:** Export the Supabase anonymous key or service role key

### Issue: "NHOST_ADMIN_SECRET not set"
**Solution:** Get admin secret from Nhost console and export it

### Issue: "Bucket does not exist"
**Solution:** Create the bucket in Nhost console first

### Issue: "Upload failed: 403 Forbidden"
**Solution:** Check bucket permissions are set to allow uploads

### Issue: "Download failed: 404 Not Found"
**Solution:** File might not exist in Supabase Storage - check the URL manually

## Migration Log

After migration, check the log file for detailed results:
```
nhost/migrations/storage_migration_log.json
```

This file contains:
- Start/end times
- Success/failure counts
- List of failed files with error messages

## Rollback Plan

If something goes wrong:
1. Files in Supabase Storage remain unchanged (read-only operation)
2. Database URLs haven't been updated yet (manual SQL execution)
3. You can delete files from Nhost Storage and retry
4. Temporary downloaded files can be reused if kept

## Estimated Time

- **Download**: ~5-10 minutes (104 files, depends on network)
- **Upload**: ~10-15 minutes (104 files, depends on network)
- **Total**: ~15-25 minutes

## Next Steps After Migration

1. ✅ Update database URLs (run generated SQL)
2. ✅ Update application code to use Nhost Storage URLs
3. ✅ Test file access on all platforms (Android, iOS, Web, Desktop)
4. ✅ Update environment variables in application
5. ✅ Run E2E tests to verify file uploads/downloads work

## Need Help?

If you encounter issues:
1. Check the migration log: `nhost/migrations/storage_migration_log.json`
2. Review failed files and error messages
3. Verify environment variables are set correctly
4. Check Nhost Storage console for uploaded files
5. Test file URLs manually in browser

