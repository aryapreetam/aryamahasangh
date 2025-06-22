#!/bin/bash

# Script to fix duplicate memberCollection fields in Address type
# The Address type has two references to memberCollection due to both address_id and temp_address_id
# foreign keys from the member table. This script renames the second occurrence to memberWithTempAddressCollection.

SCHEMA_FILE="composeApp/src/commonMain/graphql/schema.graphqls"

echo "🔧 Fixing duplicate memberCollection fields in Address type..."

# Check if schema file exists
if [ ! -f "$SCHEMA_FILE" ]; then
    echo "❌ Schema file not found at $SCHEMA_FILE"
    echo "Please run the Apollo schema download first:"
    echo "  cd composeApp && ../gradlew downloadApolloSchema"
    exit 1
fi

# Check if we need to fix the duplicate
MEMBER_COLLECTION_COUNT=$(grep -c "memberCollection(" "$SCHEMA_FILE")

if [ "$MEMBER_COLLECTION_COUNT" -lt 2 ]; then
    echo "✅ No duplicate memberCollection fields found in schema"
    exit 0
fi

echo "📋 Found $MEMBER_COLLECTION_COUNT memberCollection fields, checking Address type..."

# Extract the Address type definition
ADDRESS_TYPE_START=$(grep -n "type Address implements Node" "$SCHEMA_FILE" | cut -d: -f1)

if [ -z "$ADDRESS_TYPE_START" ]; then
    echo "❌ Could not find Address type definition in schema"
    exit 1
fi

# Find the end of Address type (next line that starts with 'type ' or 'enum ' or 'input ' or end of file)
ADDRESS_TYPE_END=$(tail -n +$((ADDRESS_TYPE_START + 1)) "$SCHEMA_FILE" | grep -n "^type \|^enum \|^input \|^interface \|^union \|^scalar " | head -1 | cut -d: -f1)

if [ -z "$ADDRESS_TYPE_END" ]; then
    # If no next type found, it goes to end of file
    ADDRESS_TYPE_END=$(wc -l < "$SCHEMA_FILE")
    ADDRESS_TYPE_END=$((ADDRESS_TYPE_END - ADDRESS_TYPE_START))
else
    ADDRESS_TYPE_END=$((ADDRESS_TYPE_END - 1))
fi

# Extract Address type content
ADDRESS_TYPE_CONTENT=$(sed -n "${ADDRESS_TYPE_START},$((ADDRESS_TYPE_START + ADDRESS_TYPE_END))p" "$SCHEMA_FILE")

# Count memberCollection occurrences in Address type
ADDRESS_MEMBER_COLLECTION_COUNT=$(echo "$ADDRESS_TYPE_CONTENT" | grep -c "memberCollection(")

if [ "$ADDRESS_MEMBER_COLLECTION_COUNT" -lt 2 ]; then
    echo "✅ Address type only has $ADDRESS_MEMBER_COLLECTION_COUNT memberCollection field, no changes needed"
    exit 0
fi

echo "🔄 Address type has $ADDRESS_MEMBER_COLLECTION_COUNT memberCollection fields, fixing..."

# Create a backup
cp "$SCHEMA_FILE" "$SCHEMA_FILE.backup"

# Use sed to replace the second occurrence of memberCollection in the Address type
# This approach finds the Address type and processes only that section
python3 << 'EOF'
import re
import sys

# Read the schema file
with open('composeApp/src/commonMain/graphql/schema.graphqls', 'r') as f:
    content = f.read()

# Find the Address type definition using regex
address_pattern = r'(type Address implements Node \{[^}]+\})'
address_match = re.search(address_pattern, content, re.DOTALL)

if not address_match:
    print("❌ Could not find Address type definition")
    sys.exit(1)

address_content = address_match.group(1)

# Find all memberCollection occurrences in the Address type
member_collection_pattern = r'memberCollection\([^)]*\): MemberConnection'
matches = list(re.finditer(member_collection_pattern, address_content))

if len(matches) < 2:
    print(f"✅ Address type only has {len(matches)} memberCollection field(s), no changes needed")
    sys.exit(0)

print(f"🔧 Found {len(matches)} memberCollection fields in Address type")

# Replace the second occurrence
def replace_second_occurrence(match):
    global replacement_count
    if replacement_count == 0:
        replacement_count += 1
        return match.group(0)  # Keep first occurrence
    else:
        return match.group(0).replace('memberCollection', 'memberWithTempAddressCollection')

replacement_count = 0
updated_address_content = re.sub(member_collection_pattern, replace_second_occurrence, address_content)

# Replace the Address type in the full content
updated_content = content.replace(address_content, updated_address_content)

# Write back to file
with open('composeApp/src/commonMain/graphql/schema.graphqls', 'w') as f:
    f.write(updated_content)

print("✅ Successfully renamed second memberCollection to memberWithTempAddressCollection in Address type")
EOF

if [ $? -eq 0 ]; then
    echo "✅ Schema file has been updated successfully!"
    echo "📄 Backup saved as $SCHEMA_FILE.backup"
else
    echo "❌ Failed to update schema file"
    # Restore backup if something went wrong
    if [ -f "$SCHEMA_FILE.backup" ]; then
        cp "$SCHEMA_FILE.backup" "$SCHEMA_FILE"
        echo "🔄 Restored original schema file from backup"
    fi
    exit 1
fi
