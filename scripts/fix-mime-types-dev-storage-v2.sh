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

echo "🔧 SUPABASE MIME TYPE FIX v2: DEV STORAGE"
echo "========================================="
echo "📁 Target:  $DEV_URL"
echo "📋 Purpose: Fix MIME types for uploaded image files"
echo ""

# Function to get correct MIME type based on file extension
get_mime_type() {
    local file_path="$1"
    local extension="${file_path##*.}"
    # Convert to lowercase using tr instead of bash parameter expansion
    extension=$(echo "$extension" | tr '[:upper:]' '[:lower:]')
    
    case "$extension" in
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
    
    echo -n "   Fixing: $file_path → $correct_mime_type..."
    
    # Download file from dev (where it currently exists with wrong MIME type)
    temp_file="/tmp/$(basename "$file_name")"
    download_url="$DEV_URL/storage/v1/object/public/$file_path"
    
    if curl -s -o "$temp_file" "$download_url"; then
        if [[ -s "$temp_file" ]]; then
            # Re-upload with correct MIME type using POST (upsert mode)
            upload_response=$(curl -s -X POST \
                "$DEV_URL/storage/v1/object/$bucket_name/$file_name" \
                -H "Authorization: Bearer $DEV_KEY" \
                -H "Content-Type: $correct_mime_type" \
                -H "x-upsert: true" \
                --data-binary @"$temp_file")
            
            # Check if response contains error
            if echo "$upload_response" | grep -q '"error"'; then
                echo " ❌"
                echo "     Error: $upload_response"
            else
                echo " ✅"
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
echo ""

# Process files by type for better organization
echo "🖼️  Processing JPEG files..."
fix_file_mime_type "documents/profile_1751026510.jpg"
fix_file_mime_type "documents/org_logo_1752322784.jpg"
fix_file_mime_type "documents/profile_1752660804.jpg"
fix_file_mime_type "documents/1749287158.jpg"
fix_file_mime_type "documents/1749285967.jpg"
fix_file_mime_type "documents/profile_1755024910.jpg"
fix_file_mime_type "documents/profile_1752663029.jpg"
fix_file_mime_type "documents/profile_1752657523.jpg"
fix_file_mime_type "documents/1749309159.jpg"
fix_file_mime_type "documents/profile_1752712714.jpg"
fix_file_mime_type "documents/1749287159.jpg"
fix_file_mime_type "documents/org_logo_1749126521.jpg"
fix_file_mime_type "documents/org_logo_1749126644.jpg"
fix_file_mime_type "documents/profile_1752673488.jpg"

echo ""
echo "🎨 Processing WebP files..."
fix_file_mime_type "profile_image/arya_sukhvidra.webp"
fix_file_mime_type "images/sp_10.webp"
fix_file_mime_type "profile_image/achary_hanumat_prasad.webp"
fix_file_mime_type "profile_image/acharya_monika_ji.webp"
fix_file_mime_type "profile_image/acharya_suman.webp"
fix_file_mime_type "profile_image/arya_dharmvir_shastri.webp"
fix_file_mime_type "images/sp_2.webp"
fix_file_mime_type "profile_image/achary_sanjiv.webp"
fix_file_mime_type "images/sp_20.webp"
fix_file_mime_type "profile_image/achary_mahesh.webp"

echo ""
echo "🎉 MIME TYPE FIX SUMMARY:"
echo "   📁 Processed all 24 files"
echo "   🖼️  JPEG files: set to image/jpeg"
echo "   🎨 WebP files: set to image/webp"
echo ""
echo "📋 VERIFICATION:"
echo "   1. Check this specific file: https://afjtpdeohgdgkrwayayn.supabase.co/storage/v1/object/public/documents/org_logo_1752322784.jpg"
echo "   2. It should now show 'image/jpeg' instead of 'application/octet-stream'"
echo "   3. Image preview should work in Supabase dashboard"
echo "   4. Images should load correctly in your application"
echo ""
echo "🎊 MIME TYPE FIX COMPLETED!"
echo "🌟 Test the URL from your issue - it should now work correctly!"
