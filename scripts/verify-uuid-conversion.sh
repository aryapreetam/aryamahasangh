#!/bin/bash
# Verify UUID Conversion
# Checks if storage URLs were successfully converted to UUID-only format

# Configuration - Update these values
ADMIN_SECRET='<YOUR_NHOST_ADMIN_SECRET>'
SUBDOMAIN='<YOUR_NHOST_SUBDOMAIN>'
REGION='<YOUR_NHOST_REGION>'

echo "🔍 Verifying UUID Conversion..."
echo ""

# Check member.profile_image
echo "Sample member.profile_image values:"
curl -s -X POST \
    "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
    -H "Content-Type: application/json" \
    -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
    -d '{"type":"run_sql","args":{"sql":"SELECT profile_image FROM member WHERE profile_image IS NOT NULL LIMIT 5;"}}' | jq -r '.result[1:] | .[] | .[0]'

echo ""
echo "Checking format..."
curl -s -X POST \
    "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
    -H "Content-Type: application/json" \
    -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
    -d '{"type":"run_sql","args":{"sql":"SELECT COUNT(*) as uuid_format FROM member WHERE profile_image ~ '\''^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$'\'';"}}' | jq -r '.result[1][0]' | xargs -I {} echo "Members with UUID-only format: {}"

curl -s -X POST \
    "https://${SUBDOMAIN}.hasura.${REGION}.nhost.run/v2/query" \
    -H "Content-Type: application/json" \
    -H "x-hasura-admin-secret: ${ADMIN_SECRET}" \
    -d '{"type":"run_sql","args":{"sql":"SELECT COUNT(*) as url_format FROM member WHERE profile_image LIKE '\''https://%'\'';"}}' | jq -r '.result[1][0]' | xargs -I {} echo "Members still with full URLs: {}"

