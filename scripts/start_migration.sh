#!/bin/bash
# Simple wrapper to run the migration with visible output

cd /Users/preetam/workspace/AryaMahasangh

echo "Starting Storage Migration..."
echo "This will migrate 104 files from Supabase to Nhost"
echo ""

python3 scripts/migrate_storage_stdlib.py

echo ""
echo "Migration completed!"

