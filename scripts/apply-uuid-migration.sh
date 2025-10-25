#!/bin/bash
# Apply UUID-Only Storage Format Migration
# Converts full URLs to UUID-only format (Nhost best practice)

# Configuration - Update these values
ADMIN_SECRET='<YOUR_NHOST_ADMIN_SECRET>'
SUBDOMAIN='<YOUR_NHOST_SUBDOMAIN>'
REGION='<YOUR_NHOST_REGION>'

echo "🔧 Converting URLs to UUIDs - Applying Migration"
echo "================================================="
echo ""

# Function to execute SQL
execute_sql() {
    local sql="$1"
    local description="$2"

    echo "  📝 $description"

    curl -s -X POST \
        "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
        -H "Content-Type: application/json" \
        -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
        -d "{\"type\":\"run_sql\",\"args\":{\"sql\":$(jq -Rs . <<< "$sql")}}" | jq -c '{result_type, error}'
}

echo "Step 1: Updating member.profile_image..."
execute_sql "UPDATE member SET profile_image = substring(profile_image from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}') WHERE profile_image LIKE 'https://%';" "Extracting UUID from URL"

echo ""
echo "Step 2: Updating organisation.logo..."
execute_sql "UPDATE organisation SET logo = substring(logo from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}') WHERE logo LIKE 'https://%';" "Extracting UUID from URL"

echo ""
echo "Step 3: Updating learning.thumbnail_url..."
execute_sql "UPDATE learning SET thumbnail_url = substring(thumbnail_url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}') WHERE thumbnail_url LIKE 'https://%';" "Extracting UUID from URL"

echo ""
echo "Step 4: Updating activities.media_files (arrays)..."
execute_sql "UPDATE activities SET media_files = array(SELECT substring(url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}') FROM unnest(media_files) AS url) WHERE EXISTS (SELECT 1 FROM unnest(media_files) AS url WHERE url LIKE 'https://%');" "Extracting UUIDs from array"

echo ""
echo "Step 5: Updating activities.overview_media_urls (arrays)..."
execute_sql "UPDATE activities SET overview_media_urls = array(SELECT substring(url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}') FROM unnest(overview_media_urls) AS url) WHERE EXISTS (SELECT 1 FROM unnest(overview_media_urls) AS url WHERE url LIKE 'https://%');" "Extracting UUIDs from array"

echo ""
echo "Step 6: Updating family.photos (arrays)..."
execute_sql "UPDATE family SET photos = array(SELECT substring(url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}') FROM unnest(photos) AS url) WHERE EXISTS (SELECT 1 FROM unnest(photos) AS url WHERE url LIKE 'https://%');" "Extracting UUIDs from array"

echo ""
echo "✅ Migration applied!"
echo ""
echo "🔍 Verifying conversion..."

# Verification
execute_sql "SELECT 'member.profile_image' as column_name, COUNT(*) as uuid_count FROM member WHERE profile_image ~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\$' UNION ALL SELECT 'activities.media_files', COUNT(*) FROM activities WHERE EXISTS (SELECT 1 FROM unnest(media_files) AS url WHERE url ~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\$');" "Verification"

echo ""
echo "🎉 All done! Database now stores UUID-only format."

