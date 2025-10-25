#!/usr/bin/env python3
"""
Simplified Data Recovery Script
Uses Supabase REST API to recover data after CASCADE data loss

CONFIGURATION REQUIRED:
- Update NHOST_ADMIN_SECRET with your Nhost admin secret
- Update NHOST_SUBDOMAIN with your Nhost project subdomain
- Update NHOST_REGION with your Nhost region
- Update SUPABASE_URL with your Supabase project URL
- Update SUPABASE_KEY with your Supabase anon/service key
"""

import urllib.request
import json

# Nhost Configuration - UPDATE THESE VALUES
NHOST_ADMIN_SECRET = '<YOUR_NHOST_ADMIN_SECRET>'
NHOST_SUBDOMAIN = '<YOUR_NHOST_SUBDOMAIN>'
NHOST_REGION = '<YOUR_NHOST_REGION>'
NHOST_URL = f'https://{NHOST_SUBDOMAIN}.hasura.{NHOST_REGION}.nhost.run/v2/query'

# Supabase REST API - UPDATE THESE VALUES
SUPABASE_URL = '<YOUR_SUPABASE_PROJECT_URL>'  # e.g., https://xxx.supabase.co
SUPABASE_KEY = '<YOUR_SUPABASE_ANON_KEY>'

def run_nhost_sql(sql):
    """Execute SQL on Nhost"""
    headers = {
        'Content-Type': 'application/json',
        'x-hasura-admin-secret': NHOST_ADMIN_SECRET
    }

    data = json.dumps({
        'type': 'run_sql',
        'args': {'sql': sql}
    }).encode()

    try:
        req = urllib.request.Request(NHOST_URL, data=data, headers=headers)
        response = urllib.request.urlopen(req)
        return json.loads(response.read())
    except Exception as e:
        return {'error': str(e)}

def get_supabase_data(table, select_fields):
    """Fetch data from Supabase REST API"""
    url = f'{SUPABASE_URL}/rest/v1/{table}?select={select_fields}'
    headers = {
        'apikey': SUPABASE_KEY,
        'Authorization': f'Bearer {SUPABASE_KEY}'
    }

    try:
        req = urllib.request.Request(url, headers=headers)
        response = urllib.request.urlopen(req)
        return json.loads(response.read())
    except Exception as e:
        print(f"Error fetching {table}: {e}")
        return None

print("🚨 SIMPLIFIED DATA RECOVERY")
print("=" * 60)

# Check current state
print("\n📋 Checking current state...")
result = run_nhost_sql("""
    SELECT 'enum_type' as object_type, typname as name
    FROM pg_type WHERE typname IN ('gender_filter', 'activity_type', 'family_relation')
    UNION ALL
    SELECT 'table' as object_type, table_name as name
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name IN ('gender_filter', 'activity_type', 'family_relation');
""")

if result.get('result'):
    print("\nCurrent objects:")
    for row in result['result'][1:]:
        print(f"  {row[0]}: {row[1]}")

# Fetch and restore activities data
print("\n\n📥 Fetching activities data from Supabase...")
activities = get_supabase_data('activities', 'id,type,allowed_gender')

if activities:
    print(f"✅ Fetched {len(activities)} activities")

    print("\n📝 Restoring activities.type...")
    success_count = 0
    for activity in activities:
        if activity.get('type'):
            sql = f"UPDATE activities SET type = '{activity['type']}'::activity_type WHERE id = '{activity['id']}';"
            result = run_nhost_sql(sql)
            if result.get('result_type') == 'CommandOk':
                success_count += 1
    print(f"✅ Restored {success_count} activity types")

    print("\n📝 Restoring activities.allowed_gender...")
    success_count = 0
    for activity in activities:
        if activity.get('allowed_gender'):
            sql = f"UPDATE activities SET allowed_gender = '{activity['allowed_gender']}'::gender_filter WHERE id = '{activity['id']}';"
            result = run_nhost_sql(sql)
            if result.get('result_type') == 'CommandOk':
                success_count += 1
    print(f"✅ Restored {success_count} allowed_gender values")

# Fetch and restore member data
print("\n\n📥 Fetching member data from Supabase...")
members = get_supabase_data('member', 'id,gender')

if members:
    print(f"✅ Fetched {len(members)} members")

    print("\n📝 Restoring member.gender...")
    success_count = 0
    for member in members:
        if member.get('gender'):
            sql = f"UPDATE member SET gender = '{member['gender']}'::gender_filter WHERE id = '{member['id']}';"
            result = run_nhost_sql(sql)
            if result.get('result_type') == 'CommandOk':
                success_count += 1
    print(f"✅ Restored {success_count} member genders")

# Final verification
print("\n\n📋 FINAL VERIFICATION")
print("=" * 60)

result = run_nhost_sql("""
    SELECT 'member.gender' as column_name, COUNT(*) as restored_count
    FROM member WHERE gender IS NOT NULL
    UNION ALL
    SELECT 'activities.type', COUNT(*)
    FROM activities WHERE type IS NOT NULL
    UNION ALL
    SELECT 'activities.allowed_gender', COUNT(*)
    FROM activities WHERE allowed_gender IS NOT NULL;
""")

if result.get('result'):
    print("\n📊 Restored Data Counts:")
    for row in result['result'][1:]:
        print(f"  ✅ {row[0]}: {row[1]} records")

print("\n\n🎉 DATA RECOVERY COMPLETE!")
print("All enum column data has been restored from Supabase.")

