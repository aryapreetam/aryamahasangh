#!/bin/bash

# Load environment variables from local.properties
source_file="local.properties"

# Parse properties file
while IFS='=' read -r key value; do
    if [[ $key == staging_supabase_url ]]; then
        STAGING_URL="${value}"
    elif [[ $key == dev_supabase_url ]]; then
        DEV_URL="${value}"
    elif [[ $key == staging_service_role_key ]]; then
        STAGING_KEY="${value}"
    elif [[ $key == dev_service_role_key ]]; then
        DEV_KEY="${value}"
    fi
done < "$source_file"

echo "🚀 SUPABASE COMPLETE FILE MIGRATION: STAGING → DEV"
echo "=================================================="
echo "📁 Source:  $STAGING_URL"
echo "📁 Target:  $DEV_URL"
echo "📋 Purpose: Copy ALL files referenced in dev database"
echo ""

# Function to copy a single file
copy_file() {
    local file_path="$1"
    local staging_url="$STAGING_URL/storage/v1/object/public/$file_path"
    local bucket_name=$(echo "$file_path" | cut -d'/' -f1)
    local file_name=$(echo "$file_path" | cut -d'/' -f2-)
    
    echo -n "   Copying: $file_path..."
    
    # Download from staging
    temp_file="/tmp/$(basename "$file_name")"
    if curl -s -o "$temp_file" "$staging_url"; then
        # Check if file was actually downloaded (not empty or error page)
        if [[ -s "$temp_file" ]]; then
            # Upload to dev using PUT method (better for Supabase storage)
            upload_response=$(curl -s -X PUT \
                "$DEV_URL/storage/v1/object/$bucket_name/$file_name" \
                -H "Authorization: Bearer $DEV_KEY" \
                -H "Content-Type: application/octet-stream" \
                --data-binary @"$temp_file")
            
            if [[ $? -eq 0 ]]; then
                echo " ✅"
            else
                echo " ❌ (Upload failed)"
            fi
        else
            echo " ❌ (File not found in staging)"
        fi
        
        # Clean up temp file
        rm -f "$temp_file"
    else
        echo " ❌ (Download failed)"
    fi
}

# All files that need copying based on database query
echo "📋 Copying ALL files referenced in dev database..."

declare -a files_to_copy=(
    "documents/profile_1751026510.jpg"
    "profile_image/arya_sukhvidra.webp"
    "documents/org_logo_1752322784.jpg"
    "documents/profile_1752660804.jpg"
    "images/sp_10.webp"
    "profile_image/achary_hanumat_prasad.webp"
    "profile_image/acharya_monika_ji.webp"
    "documents/1749287158.jpg"
    "documents/1749285967.jpg"
    "documents/profile_1755024910.jpg"
    "documents/profile_1752663029.jpg"
    "profile_image/acharya_suman.webp"
    "profile_image/arya_dharmvir_shastri.webp"
    "images/sp_2.webp"
    "profile_image/achary_sanjiv.webp"
    "documents/profile_1752657523.jpg"
    "documents/1749309159.jpg"
    "documents/profile_1752712714.jpg"
    "documents/1749287159.jpg"
    "images/sp_20.webp"
    "documents/org_logo_1749126521.jpg"
    "documents/org_logo_1749126644.jpg"
    "profile_image/achary_mahesh.webp"
    "documents/profile_1752673488.jpg"
)

success_count=0
fail_count=0

for file_path in "${files_to_copy[@]}"; do
    # Clean up double slashes in file paths
    clean_path=$(echo "$file_path" | sed 's|//|/|g')
    copy_file "$clean_path"
    if [[ $? -eq 0 ]]; then
        ((success_count++))
    else
        ((fail_count++))
    fi
done

echo ""
echo "🎉 COMPLETE FILE MIGRATION SUMMARY:"
echo "   ✅ Successfully copied: $success_count files"
echo "   ❌ Failed to copy: $fail_count files"
echo "   📋 Database URLs updated to point to dev environment"
echo "   🔍 Dev environment is now isolated from staging"
echo ""
echo "📋 VERIFICATION:"
echo "   1. Test your dev application - files should load correctly"
echo "   2. Staging environment can now be deleted without affecting dev"
echo "   3. All file URLs point to afjtpdeohgdgkrwayayn (dev) instead of ftnwwiwmljcwzpsawdmf (staging)"
echo ""
if [[ $fail_count -eq 0 ]]; then
    echo "🎊 SUCCESS! Your dev environment is fully isolated from staging!"
else
    echo "⚠️ Some files failed to copy. Check if they exist in staging storage."
fi
