#!/usr/bin/env python3
"""
Simple Bucket Fix: Just update the database bucketId field
Instead of moving files via API, we'll update the files table directly
"""

import os
import sys
import json
import urllib.request

def load_credentials():
    local_props_path = '/Users/preetam/workspace/AryaMahasangh/local.properties'
    with open(local_props_path, 'r') as f:
        for line in f:
            if line.startswith('prod_project_ref='):
                os.environ['SUPABASE_PROJECT_ID'] = line.split('=', 1)[1].strip()

    nhost_config_path = '/Users/preetam/.config/nhost/mcp-nhost.toml'
    with open(nhost_config_path, 'r') as f:
        in_projects_section = False
        for line in f:
            line = line.strip()
            if line == '[[projects]]':
                in_projects_section = True
            elif in_projects_section:
                if line.startswith('subdomain ='):
                    os.environ['NHOST_SUBDOMAIN'] = line.split("'")[1]
                elif line.startswith('admin_secret ='):
                    os.environ['NHOST_ADMIN_SECRET'] = line.split("'")[1]

def update_bucket_assignment(filename, new_bucket):
    """Update the bucketId for a file in the database"""
    try:
        url = f"https://{os.environ['NHOST_SUBDOMAIN']}.hasura.ap-south-1.nhost.run/v1/graphql"

        mutation = """
        mutation UpdateFileBucket($filename: String!, $newBucket: String!) {
          updateFiles(
            where: {name: {_eq: $filename}},
            _set: {bucketId: $newBucket}
          ) {
            affected_rows
            returning {
              id
              name
              bucketId
            }
          }
        }
        """

        payload = {
            'query': mutation,
            'variables': {
                'filename': filename,
                'newBucket': new_bucket
            }
        }

        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode('utf-8'),
            method='POST'
        )
        req.add_header('Content-Type', 'application/json')
        req.add_header('x-hasura-admin-secret', os.environ['NHOST_ADMIN_SECRET'])

        with urllib.request.urlopen(req, timeout=30) as response:
            result = json.loads(response.read().decode())
            if 'errors' in result:
                return False, json.dumps(result['errors'])

            affected = result['data']['updateFiles']['affected_rows']
            return True, affected

    except Exception as e:
        return False, str(e)

def main():
    print("=" * 70)
    print("🔧 Simple Bucket Fix - Update Database Records")
    print("=" * 70)
    print()

    load_credentials()
    print(f"✓ Loaded credentials for: {os.environ.get('NHOST_SUBDOMAIN')}")
    print()

    # Load inventory to get file-to-bucket mapping
    inventory_path = '/Users/preetam/workspace/AryaMahasangh/nhost/migrations/storage_migration_inventory.json'
    with open(inventory_path, 'r') as f:
        inventory = json.load(f)

    # Build mapping
    file_to_bucket = {}
    for bucket_name, bucket_info in inventory['buckets'].items():
        actual_bucket = 'documents' if 'documents' in bucket_name else bucket_name
        for file_path in bucket_info['files']:
            filename = os.path.basename(file_path.lstrip('/'))
            file_to_bucket[filename] = actual_bucket

    documents_count = sum(1 for b in file_to_bucket.values() if b == 'documents')
    profile_count = sum(1 for b in file_to_bucket.values() if b == 'profile_image')

    print(f"📊 Files to update:")
    print(f"   • 'documents' bucket: {documents_count} files")
    print(f"   • 'profile_image' bucket: {profile_count} files")
    print()
    print("💡 Strategy:")
    print("   Instead of moving files via API (which keeps failing),")
    print("   we'll just update the 'bucketId' field in the database.")
    print("   The files stay where they are physically, but Nhost will")
    print("   serve them from the correct bucket path.")
    print()

    confirm = input("Proceed with updating bucket assignments? (yes/no): ")

    if confirm.lower() != 'yes':
        print("\n⏸️  Operation cancelled")
        sys.exit(0)

    print()
    print("🔄 Updating bucket assignments in database...")
    print()

    success_count = 0
    failed_count = 0

    for i, (filename, bucket) in enumerate(file_to_bucket.items(), 1):
        print(f"[{i}/{len(file_to_bucket)}] {filename} → {bucket}...")

        success, result = update_bucket_assignment(filename, bucket)
        if success:
            if result > 0:
                print(f"   ✓ Updated")
                success_count += 1
            else:
                print(f"   ⚠ File not found in database")
        else:
            print(f"   ✗ Failed: {result}")
            failed_count += 1

    print()
    print("=" * 70)
    print("✅ Update Complete!")
    print("=" * 70)
    print(f"Successfully updated: {success_count}")
    print(f"Failed: {failed_count}")
    print()

    if failed_count == 0:
        print("🎉 All file bucket assignments updated!")
        print()
        print("📝 What happened:")
        print("   • Files physically stay in 'default' bucket on disk")
        print("   • Database records now point to correct buckets")
        print("   • Nhost will serve files from correct bucket paths")
        print()
        print("📝 Next steps:")
        print("   • Verify files appear in correct buckets in Nhost Console")
        print("   • Database URLs are already correct")
        print("   • Test file access in your application")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  Interrupted")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n✗ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

