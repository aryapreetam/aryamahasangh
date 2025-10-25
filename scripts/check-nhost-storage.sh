#!/bin/bash
# Check Nhost Storage Files
# Verifies what files exist in Nhost storage

# Configuration - Update these values
ADMIN_SECRET='<YOUR_NHOST_ADMIN_SECRET>'
SUBDOMAIN='<YOUR_NHOST_SUBDOMAIN>'
REGION='<YOUR_NHOST_REGION>'

echo "🔍 Checking Nhost Storage Files..."
echo ""

# Check if files exist in storage.files table
echo "📋 Querying storage.files table..."
curl -s -X POST \
    "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
    -H "Content-Type: application/json" \
    -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
    -d '{"type":"run_sql","args":{"sql":"SELECT COUNT(*) as total_files FROM storage.files;"}}' | jq -r '.result[1][0]' | xargs -I {} echo "Total files in storage: {}"

echo ""

# Get sample files
echo "📄 Sample files in storage:"
curl -s -X POST \
    "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
    -H "Content-Type: application/json" \
    -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
    -d '{"type":"run_sql","args":{"sql":"SELECT id, name, bucket_id, size FROM storage.files LIMIT 10;"}}' | jq .

echo ""

# Check buckets
echo "🗂️  Storage buckets:"
curl -s -X POST \
    "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
    -H "Content-Type: application/json" \
    -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
    -d '{"type":"run_sql","args":{"sql":"SELECT id, download_expiration, min_upload_file_size, max_upload_file_size FROM storage.buckets;"}}' | jq .

echo ""

# Test file access - try to get a file URL
echo "🔗 Testing file URL format..."
SAMPLE_FILE_ID=$(curl -s -X POST \
    "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
    -H "Content-Type: application/json" \
    -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
    -d '{"type":"run_sql","args":{"sql":"SELECT id FROM storage.files LIMIT 1;"}}' | jq -r '.result[1][0]')

if [ ! -z "$SAMPLE_FILE_ID" ]; then
    echo "Sample file ID: $SAMPLE_FILE_ID"
    echo "Correct URL format: https://${SUBDOMAIN}.storage.${REGION}.nhost.run/v1/files/${SAMPLE_FILE_ID}"
else
    echo "⚠️  No files found in storage!"
fi

