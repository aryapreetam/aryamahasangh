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

echo "🔧 SUPABASE MIME TYPE FIX: DEV STORAGE"
echo "====================================="
echo "📁 Target:  $DEV_URL"
echo "📋 Purpose: Fix MIME types for uploaded image files"
echo ""

# Function to get correct MIME type based on file extension
get_mime_type() {
    local file_path="$1"
    local extension="${file_path##*.}"
    
    case "${extension,,}" in
        jpg|jpeg) echo "image/jpeg" ;;
        png) echo "image/png" ;;
        webp) echo "image/webp" ;;
        gif) echo "image/gif" ;;
        svg) echo "image/svg+xml" ;;
        *) echo "application/octet-stream" ;;
    esac
}

# Function to fix MIME type for a single file
fix_file_mime_type() {
    local file_path="$1"
    local correct_mime_type=$(get_mime_type "$file_path")
    local bucket_name=$(echo "$file_path" | cut -d'/' -f1)
    local file_name=$(echo "$file_path" | cut -d'/' -f2-)
    
    echo -n "   Fixing MIME type for: $file_path ($correct_mime_type)..."
    
    # Download file from dev (where it currently exists with wrong MIME type)
    temp_file="/tmp/$(basename "$file_name")"
    download_url="$DEV_URL/storage/v1/object/public/$file_path"
    
    if curl -s -o "$temp_file" "$download_url"; then
        if [[ -s "$temp_file" ]]; then
            # Re-upload with correct MIME type using upsert (overwrite)
            upload_response=$(curl -s -X PUT \
                "$DEV_URL/storage/v1/object/$bucket_name/$file_name" \
                -H "Authorization: Bearer $DEV_KEY" \
                -H "Content-Type: $correct_mime_type" \
                -H "x-upsert: true" \
                --data-binary @"$temp_file")
            
            if [[ $? -eq 0 ]]; then
                echo " ✅"
            else
                echo " ❌ (Upload failed)"
                echo "     Response: $upload_response"
            fi
        else
            echo " ❌ (File not found or empty)"
        fi
        
        # Clean up temp file
        rm -f "$temp_file"
    else
        echo " ❌ (Download failed)"
    fi
}

echo "📋 Fixing MIME types for all uploaded files..."

# All files that were uploaded (same list as before)
declare -a files_to_fix=(
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

for file_path in "${files_to_fix[@]}"; do
    # Clean up double slashes in file paths
    clean_path=$(echo "$file_path" | sed 's|//|/|g')
    fix_file_mime_type "$clean_path"
    if [[ $? -eq 0 ]]; then
        ((success_count++))
    else
        ((fail_count++))
    fi
done

echo ""
echo "🎉 MIME TYPE FIX SUMMARY:"
echo "   ✅ Successfully fixed: $success_count files"
echo "   ❌ Failed to fix: $fail_count files"
echo ""
echo "📋 VERIFICATION:"
echo "   1. Check Supabase dashboard - files should now show correct MIME types"
echo "   2. Test image previews in dashboard"
echo "   3. Verify images load correctly in your application"
echo ""
if [[ $fail_count -eq 0 ]]; then
    echo "🎊 SUCCESS! All files now have correct MIME types!"
    echo "🖼️  Images should now display correctly in browsers and Supabase dashboard"
else
    echo "⚠️ Some files failed to update. Check the error messages above."
fi
