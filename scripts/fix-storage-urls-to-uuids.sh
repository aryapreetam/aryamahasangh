#!/bin/bash
# Fix All Storage URLs to Use UUID Format
# Converts path-based URLs to UUID-based URLs for Nhost storage

# Configuration - Update these values
ADMIN_SECRET='<YOUR_NHOST_ADMIN_SECRET>'
SUBDOMAIN='<YOUR_NHOST_SUBDOMAIN>'
REGION='<YOUR_NHOST_REGION>'

echo "🔧 Fixing Nhost Storage URLs - Phase 2"
echo "======================================"
echo ""
echo "Problem: Database has path-based URLs, but Nhost uses UUID-based URLs"
echo "Solution: Map filenames to UUIDs and update all database references"
echo ""

# Function to execute SQL
execute_sql() {
    local sql="$1"
    curl -s -X POST \
        "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
        -H "Content-Type: application/json" \
        -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
        -d "{\"type\":\"run_sql\",\"args\":{\"sql\":$(echo "$sql" | jq -Rs .)}}"
}

echo "📊 Step 1: Analyzing current state..."
echo ""

# Get total file count
TOTAL_FILES=$(execute_sql "SELECT COUNT(*) FROM storage.files;" | jq -r '.result[1][0]')
echo "  ✓ Total files in Nhost storage: $TOTAL_FILES"

# Count affected records
MEMBER_COUNT=$(execute_sql "SELECT COUNT(*) FROM member WHERE profile_image IS NOT NULL AND profile_image != '';" | jq -r '.result[1][0]')
ACTIVITIES_MEDIA=$(execute_sql "SELECT COUNT(*) FROM activities WHERE media_files IS NOT NULL AND array_length(media_files, 1) > 0;" | jq -r '.result[1][0]')
ACTIVITIES_OVERVIEW=$(execute_sql "SELECT COUNT(*) FROM activities WHERE overview_media_urls IS NOT NULL AND array_length(overview_media_urls, 1) > 0;" | jq -r '.result[1][0]')

echo "  ✓ Members with profile images: $MEMBER_COUNT"
echo "  ✓ Activities with media files: $ACTIVITIES_MEDIA"
echo "  ✓ Activities with overview media: $ACTIVITIES_OVERVIEW"

echo ""
echo "📝 Step 2: Creating filename -> UUID mapping..."
echo ""

# Create a temporary table to store the mapping
echo "  Creating mapping table..."
execute_sql "
CREATE TEMP TABLE file_mapping AS
SELECT
    name as filename,
    id as file_id
FROM storage.files;
" > /dev/null

echo "  ✓ Mapping table created"

echo ""
echo "🔄 Step 3: Updating member.profile_image..."
execute_sql "
UPDATE member m
SET profile_image = 'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/' || fm.file_id
FROM file_mapping fm
WHERE m.profile_image IS NOT NULL
  AND m.profile_image != ''
  AND m.profile_image LIKE '%' || fm.filename;
"

UPDATED=$(execute_sql "SELECT COUNT(*) FROM member WHERE profile_image LIKE '%/v1/files/%' AND profile_image NOT LIKE '%/documents/%';" | jq -r '.result[1][0]')
echo "  ✓ Updated $UPDATED member profile images"

echo ""
echo "🔄 Step 4: Updating activities.media_files (array column)..."
execute_sql "
UPDATE activities a
SET media_files = (
    SELECT array_agg(
        'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/' ||
        (SELECT file_id FROM file_mapping WHERE a_url LIKE '%' || filename LIMIT 1)
    )
    FROM unnest(a.media_files) AS a_url
    WHERE (SELECT file_id FROM file_mapping WHERE a_url LIKE '%' || filename LIMIT 1) IS NOT NULL
)
WHERE media_files IS NOT NULL AND array_length(media_files, 1) > 0;
"

echo "  ✓ Updated activities.media_files arrays"

echo ""
echo "🔄 Step 5: Updating activities.overview_media_urls (array column)..."
execute_sql "
UPDATE activities a
SET overview_media_urls = (
    SELECT array_agg(
        'https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/' ||
        (SELECT file_id FROM file_mapping WHERE a_url LIKE '%' || filename LIMIT 1)
    )
    FROM unnest(a.overview_media_urls) AS a_url
    WHERE (SELECT file_id FROM file_mapping WHERE a_url LIKE '%' || filename LIMIT 1) IS NOT NULL
)
WHERE overview_media_urls IS NOT NULL AND array_length(overview_media_urls, 1) > 0;
"

echo "  ✓ Updated activities.overview_media_urls arrays"

echo ""
echo "✅ All URLs updated successfully!"

