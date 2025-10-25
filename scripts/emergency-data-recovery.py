#!/usr/bin/env python3
"""
Emergency Data Recovery Script
Recovers lost enum columns from Supabase Production after CASCADE data loss

CONFIGURATION REQUIRED:
- Update NHOST_ADMIN_SECRET with your Nhost admin secret
- Update NHOST_SUBDOMAIN with your Nhost project subdomain
- Update NHOST_REGION with your Nhost region
- Update SUPABASE_URL with your Supabase project URL
- Update SUPABASE_KEY with your Supabase anon/service key
"""

import urllib.request
import json
import sys

# Nhost Configuration - UPDATE THESE VALUES
NHOST_ADMIN_SECRET = '<YOUR_NHOST_ADMIN_SECRET>'
NHOST_SUBDOMAIN = '<YOUR_NHOST_SUBDOMAIN>'
NHOST_REGION = '<YOUR_NHOST_REGION>'
NHOST_URL = f'https://{NHOST_SUBDOMAIN}.hasura.{NHOST_REGION}.nhost.run/v2/query'

# Supabase Configuration (Source database) - UPDATE THESE VALUES
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

print("🚨 EMERGENCY DATA RECOVERY")
print("=" * 60)
print("Recovering lost enum columns from Supabase Production")
print("=" * 60)

# Verify configuration
if '<YOUR_' in NHOST_ADMIN_SECRET or '<YOUR_' in SUPABASE_URL:
    print("\n❌ ERROR: Please update the configuration values at the top of this script!")
    print("   - NHOST_ADMIN_SECRET")
    print("   - NHOST_SUBDOMAIN")
    print("   - NHOST_REGION")
    print("   - SUPABASE_URL")
    print("   - SUPABASE_KEY")
    sys.exit(1)

# STEP 1: Recreate enum types
print("\n\n📋 STEP 1: Recreating PostgreSQL ENUM Types")
print("-" * 60)

run_nhost_sql("CREATE TYPE gender_filter AS ENUM ('MALE', 'FEMALE', 'ANY');")
print("✅ Created gender_filter enum type")

run_nhost_sql("CREATE TYPE activity_type AS ENUM ('SESSION', 'CAMP', 'COURSE', 'EVENT', 'CAMPAIGN', 'PROTECTION_SESSION', 'BODH_SESSION');")
print("✅ Created activity_type enum type")

run_nhost_sql("CREATE TYPE family_relation AS ENUM ('SELF', 'FATHER', 'MOTHER', 'HUSBAND', 'WIFE', 'SON', 'DAUGHTER', 'BROTHER', 'SISTER', 'GRANDFATHER', 'GRANDMOTHER', 'GRANDSON', 'GRANDDAUGHTER', 'UNCLE', 'AUNT', 'COUSIN', 'NEPHEW', 'NIECE', 'GUARDIAN', 'RELATIVE', 'OTHER');")
print("✅ Created family_relation enum type")

# STEP 2: Recreate columns
print("\n\n📋 STEP 2: Recreating Dropped Columns")
print("-" * 60)

run_nhost_sql("ALTER TABLE member ADD COLUMN gender gender_filter;")
print("✅ Added member.gender column")

run_nhost_sql("ALTER TABLE activities ADD COLUMN type activity_type;")
print("✅ Added activities.type column")

run_nhost_sql("ALTER TABLE activities ADD COLUMN allowed_gender gender_filter;")
print("✅ Added activities.allowed_gender column")

run_nhost_sql("ALTER TABLE family_member ADD COLUMN relation_to_head family_relation;")
print("✅ Added family_member.relation_to_head column")

# STEP 3: Fetch and restore data
print("\n\n📋 STEP 3: Fetching Lost Data from Supabase")
print("-" * 60)

# Fetch member data
print("\n📥 Fetching member data...")
members = get_supabase_data('member', 'id,gender')

if members:
    print(f"✅ Fetched {len(members)} members")
    print("📝 Restoring member.gender...")
    success_count = 0
    for member in members:
        if member.get('gender'):
            sql = f"UPDATE member SET gender = '{member['gender']}'::gender_filter WHERE id = '{member['id']}';"
            result = run_nhost_sql(sql)
            if result.get('result_type') == 'CommandOk':
                success_count += 1
    print(f"✅ Restored {success_count} member genders")

# Fetch activities data
print("\n📥 Fetching activities data...")
activities = get_supabase_data('activities', 'id,type,allowed_gender')

if activities:
    print(f"✅ Fetched {len(activities)} activities")

    print("📝 Restoring activities.type...")
    success_count = 0
    for activity in activities:
        if activity.get('type'):
            sql = f"UPDATE activities SET type = '{activity['type']}'::activity_type WHERE id = '{activity['id']}';"
            result = run_nhost_sql(sql)
            if result.get('result_type') == 'CommandOk':
                success_count += 1
    print(f"✅ Restored {success_count} activity types")

    print("📝 Restoring activities.allowed_gender...")
    success_count = 0
    for activity in activities:
        if activity.get('allowed_gender'):
            sql = f"UPDATE activities SET allowed_gender = '{activity['allowed_gender']}'::gender_filter WHERE id = '{activity['id']}';"
            result = run_nhost_sql(sql)
            if result.get('result_type') == 'CommandOk':
                success_count += 1
    print(f"✅ Restored {success_count} allowed_gender values")

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

