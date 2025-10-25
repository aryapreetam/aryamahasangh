#!/bin/bash
# Apply URL Migration via Nhost GraphQL API
# Updates database URLs from Supabase format to Nhost format

# Configuration - Update these values
ADMIN_SECRET='<YOUR_NHOST_ADMIN_SECRET>'
SUBDOMAIN='<YOUR_NHOST_SUBDOMAIN>'
REGION='<YOUR_NHOST_REGION>'

echo "🔄 Applying URL Migration via Nhost GraphQL API..."
echo ""

# Function to execute SQL via Hasura
execute_sql() {
    local sql="$1"
    local description="$2"

    echo "  📝 $description"

    curl -s -X POST \
        "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
        -H "Content-Type: application/json" \
        -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
        -d "{\"type\":\"run_sql\",\"args\":{\"sql\":$(jq -Rs . <<< "$sql")}}" | jq .
}

echo "Step 1: Updating member.profile_image"
execute_sql "UPDATE member SET profile_image = REPLACE(profile_image, 'https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/', 'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/') WHERE profile_image IS NOT NULL AND profile_image != '';" "Updating member.profile_image"

echo ""
echo "Step 2: Updating organisation.logo"
execute_sql "UPDATE organisation SET logo = REPLACE(logo, 'https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/', 'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/') WHERE logo IS NOT NULL AND logo != '';" "Updating organisation.logo"

echo ""
echo "Step 3: Updating learning.thumbnail_url"
execute_sql "UPDATE learning SET thumbnail_url = REPLACE(thumbnail_url, 'https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/', 'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/') WHERE thumbnail_url IS NOT NULL AND thumbnail_url != '';" "Updating learning.thumbnail_url"

echo ""
echo "Step 4: Updating activities.media_files"
execute_sql "UPDATE activities SET media_files = ARRAY(SELECT REPLACE(unnest(media_files), 'https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/', 'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/')) WHERE media_files IS NOT NULL AND array_length(media_files, 1) > 0;" "Updating activities.media_files"

echo ""
echo "Step 5: Updating activities.overview_media_urls"
execute_sql "UPDATE activities SET overview_media_urls = ARRAY(SELECT REPLACE(unnest(overview_media_urls), 'https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/', 'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/')) WHERE overview_media_urls IS NOT NULL AND array_length(overview_media_urls, 1) > 0;" "Updating activities.overview_media_urls"

echo ""
echo "Step 6: Updating family.photos"
execute_sql "UPDATE family SET photos = ARRAY(SELECT REPLACE(unnest(photos), 'https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/', 'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/')) WHERE photos IS NOT NULL AND array_length(photos, 1) > 0;" "Updating family.photos"

echo ""
echo "✅ Migration completed!"

