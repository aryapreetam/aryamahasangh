#!/usr/bin/env python3
"""
Storage Migration Script using only standard library (no external dependencies)
Migrates files from Supabase Storage to Nhost Storage
"""

import os
import sys
import json
import urllib.request
import urllib.error
from pathlib import Path
import time

# Configuration
SUPABASE_PROJECT_ID = #project_ref
NHOST_PROJECT_ID = #subdomain

def load_credentials():
    """Load credentials from config files"""
    # Load Supabase credentials
    local_props_path = '/Users/preetam/workspace/AryaMahasangh/local.properties'
    with open(local_props_path, 'r') as f:
        for line in f:
            if line.startswith('prod_supabase_key='):
                os.environ['SUPABASE_ANON_KEY'] = line.split('=', 1)[1].strip()
            elif line.startswith('prod_project_ref='):
                os.environ['SUPABASE_PROJECT_ID'] = line.split('=', 1)[1].strip()
            elif line.startswith('prod_service_role_key='):
                os.environ['SUPABASE_SERVICE_ROLE_KEY'] = line.split('=', 1)[1].strip()

    # Load Nhost credentials
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

def download_file(url, local_path):
    """Download a file from URL to local path"""
    try:
        os.makedirs(os.path.dirname(local_path), exist_ok=True)

        req = urllib.request.Request(url)
        if os.environ.get('SUPABASE_ANON_KEY'):
            req.add_header('apikey', os.environ['SUPABASE_ANON_KEY'])
            req.add_header('Authorization', f"Bearer {os.environ['SUPABASE_ANON_KEY']}")

        with urllib.request.urlopen(req, timeout=30) as response:
            with open(local_path, 'wb') as f:
                f.write(response.read())
        return True, None
    except Exception as e:
        return False, str(e)

def upload_file_to_nhost(local_path, bucket, file_path):
    """Upload a file to Nhost Storage"""
    try:
        # Read file content
        with open(local_path, 'rb') as f:
            file_content = f.read()

        # Clean file path - remove leading slashes
        clean_path = file_path.lstrip('/')

        # Prepare multipart form data
        boundary = '----WebKitFormBoundary' + str(int(time.time() * 1000))

        # Build multipart body - Nhost requires bucket-id as a form field
        body = []

        # Add bucket-id field FIRST (this is critical!)
        body.append(f'--{boundary}'.encode())
        body.append(b'Content-Disposition: form-data; name="bucket-id"')
        body.append(b'')
        body.append(bucket.encode())

        # Add file field
        body.append(f'--{boundary}'.encode())
        filename = os.path.basename(clean_path)
        content_type = get_content_type(filename)
        body.append(f'Content-Disposition: form-data; name="file"; filename="{filename}"'.encode())
        body.append(f'Content-Type: {content_type}'.encode())
        body.append(b'')
        body.append(file_content)

        # End boundary
        body.append(f'--{boundary}--'.encode())
        body.append(b'')

        body_bytes = b'\r\n'.join(body)

        # Create request - bucket ID is in the form data, not URL
        nhost_subdomain = os.environ['NHOST_SUBDOMAIN']
        url = f"https://{nhost_subdomain}.storage.ap-south-1.nhost.run/v1/files"

        req = urllib.request.Request(url, data=body_bytes, method='POST')
        req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
        req.add_header('Content-Length', str(len(body_bytes)))

        if os.environ.get('NHOST_ADMIN_SECRET'):
            req.add_header('x-hasura-admin-secret', os.environ['NHOST_ADMIN_SECRET'])

        with urllib.request.urlopen(req, timeout=60) as response:
            response_data = response.read().decode()
            return response.status in [200, 201], response_data
    except urllib.error.HTTPError as e:
        error_body = e.read().decode() if e.fp else str(e)
        return False, f"HTTP {e.code}: {error_body}"
    except Exception as e:
        return False, str(e)

def get_content_type(filename):
    """Get content type based on file extension"""
    ext = os.path.splitext(filename)[1].lower()
    content_types = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.webp': 'image/webp',
        '.gif': 'image/gif',
    }
    return content_types.get(ext, 'application/octet-stream')

def main():
    print("=" * 70)
    print("🚀 Storage Migration: Supabase → Nhost")
    print("=" * 70)
    print()

    # Load credentials
    print("🔧 Loading credentials...")
    load_credentials()
    print(f"✓ Supabase Project: {os.environ.get('SUPABASE_PROJECT_ID')}")
    print(f"✓ Nhost Subdomain: {os.environ.get('NHOST_SUBDOMAIN')}")
    print()

    # Load inventory
    inventory_path = 'nhost/migrations/storage_migration_inventory.json'
    with open(inventory_path, 'r') as f:
        inventory = json.load(f)

    print(f"📊 Files to migrate: {inventory['summary']['total_files']}")
    print()

    # Create temp directory
    temp_dir = 'temp_storage_migration'
    Path(temp_dir).mkdir(parents=True, exist_ok=True)

    # Track results
    success_count = 0
    failed_count = 0
    failed_files = []

    # Migrate files from each bucket
    for bucket_name, bucket_info in inventory['buckets'].items():
        if bucket_name == "documents/activity_overview":
            actual_bucket = "documents"
            files = [f"activity_overview/{f}" for f in bucket_info['files']]
        else:
            actual_bucket = bucket_name
            files = bucket_info['files']

        print(f"\n📦 Migrating bucket: {actual_bucket} ({len(files)} files)")

        for i, file_path in enumerate(files, 1):
            # Clean file path
            clean_path = file_path.lstrip('/')

            print(f"[{i}/{len(files)}] {clean_path[:50]}...")

            # Download
            download_url = f"https://{os.environ['SUPABASE_PROJECT_ID']}.supabase.co/storage/v1/object/public/{actual_bucket}/{file_path}"
            local_path = os.path.join(temp_dir, actual_bucket, clean_path)

            success, error = download_file(download_url, local_path)
            if not success:
                print(f"   ✗ Download failed: {error}")
                failed_count += 1
                failed_files.append({'file': clean_path, 'error': error, 'stage': 'download'})
                continue

            print(f"   ✓ Downloaded")

            # Upload
            success, result = upload_file_to_nhost(local_path, actual_bucket, clean_path)
            if success:
                print(f"   ✓ Uploaded")
                success_count += 1
            else:
                print(f"   ✗ Upload failed: {result}")
                failed_count += 1
                failed_files.append({'file': clean_path, 'error': result, 'stage': 'upload'})

            time.sleep(0.3)  # Rate limiting

    # Save log
    log_data = {
        'timestamp': time.strftime("%Y-%m-%d %H:%M:%S"),
        'total_files': success_count + failed_count,
        'successful': success_count,
        'failed': failed_count,
        'failed_files': failed_files
    }

    with open('nhost/migrations/storage_migration_log.json', 'w') as f:
        json.dump(log_data, f, indent=2)

    # Generate SQL
    with open('nhost/migrations/00012_update_storage_urls.sql', 'w') as f:
        f.write("-- Update storage URLs from Supabase to Nhost\n")
        f.write(f"-- Generated: {time.strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("-- Update member.profile_image URLs\n")
        f.write("UPDATE member SET profile_image = REPLACE(\n")
        f.write(f"  profile_image,\n")
        f.write(f"  'https://{os.environ['SUPABASE_PROJECT_ID']}.supabase.co/storage/v1/object/public/',\n")
        f.write(f"  'https://{os.environ['NHOST_SUBDOMAIN']}.storage.run.app/v1/files/'\n")
        f.write(") WHERE profile_image IS NOT NULL AND profile_image != '';\n")

    # Cleanup
    import shutil
    shutil.rmtree(temp_dir)

    # Summary
    print("\n" + "=" * 70)
    print("✅ Migration Complete!")
    print("=" * 70)
    print(f"Total files: {success_count + failed_count}")
    print(f"✓ Successful: {success_count}")
    print(f"✗ Failed: {failed_count}")
    print()
    print("📝 Files created:")
    print("   • nhost/migrations/storage_migration_log.json")
    print("   • nhost/migrations/00012_update_storage_urls.sql")

    return 0 if failed_count == 0 else 1

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\n\n⚠️  Migration interrupted")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n✗ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

