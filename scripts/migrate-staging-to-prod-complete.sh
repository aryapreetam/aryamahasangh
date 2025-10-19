#!/bin/bash

# Load environment variables from local.properties
source_file="local.properties"

# Parse properties file
while IFS='=' read -r key value; do
    if [[ $key == staging_supabase_url ]]; then
        STAGING_URL="${value}"
    elif [[ $key == prod_supabase_url ]]; then
        PROD_URL="${value}"
    elif [[ $key == staging_service_role_key ]]; then
        STAGING_KEY="${value}"
    elif [[ $key == prod_service_role_key ]]; then
        PROD_KEY="${value}"
    fi
done < "$source_file"

echo "🚀 PRODUCTION ENVIRONMENT ISOLATION: STAGING → PROD"
echo "==================================================="
echo "📁 Source:  $STAGING_URL (ftnwwiwmljcwzpsawdmf)"
echo "📁 Target:  $PROD_URL (jusbsyslwvrdmdwdsvfk)" 
echo "📋 Purpose: Fix prod cross-environment dependencies + file migration"
echo ""

# Function to get correct MIME type based on file extension
get_mime_type() {
    local file_path="$1"
    local extension="${file_path##*.}"
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

# Function to copy and fix MIME type for a single file
copy_and_fix_file() {
    local file_path="$1"
    local correct_mime_type=$(get_mime_type "$file_path")
    local bucket_name=$(echo "$file_path" | cut -d'/' -f1)
    local file_name=$(echo "$file_path" | cut -d'/' -f2-)
    
    # Clean up double slashes
    clean_path=$(echo "$file_path" | sed 's|//|/|g')
    clean_file_name=$(echo "$file_name" | sed 's|//|/|g')
    
    echo -n "   $clean_path → $correct_mime_type..."
    
    # Download from staging
    temp_file="/tmp/$(basename "$clean_file_name")"
    staging_url="$STAGING_URL/storage/v1/object/public/$clean_path"
    
    if curl -s -o "$temp_file" "$staging_url"; then
        if [[ -s "$temp_file" ]]; then
            # Upload to prod with correct MIME type
            upload_response=$(curl -s -X POST \
                "$PROD_URL/storage/v1/object/$bucket_name/$clean_file_name" \
                -H "Authorization: Bearer $PROD_KEY" \
                -H "Content-Type: $correct_mime_type" \
                -H "x-upsert: true" \
                --data-binary @"$temp_file")
            
            if echo "$upload_response" | grep -q '"error"'; then
                echo " ❌"
                echo "     Error: $upload_response"
                return 1
            else
                echo " ✅"
                return 0
            fi
        else
            echo " ❌ (Empty/404 in staging)"
            return 1
        fi
        
        rm -f "$temp_file"
    else
        echo " ❌ (Download failed)"
        return 1
    fi
}

# ALL FILES that need migration from staging to prod (73 files)
echo "📋 PHASE 1: Copying all files from staging to prod storage..."

declare -a prod_files=(
    "documents/1749187721.jpg"
    "documents/1749210930.jpg"
    "documents/1749213212.jpg"
    "documents/1749213779.jpg"
    "documents/1749218629.jpg"
    "documents/1749275092.jpg"
    "documents/1749284940.jpg"
    "documents/1749285149.jpg"
    "documents/1749285967.jpg"
    "documents/1749287158.jpg"
    "documents/1749287159.jpg"
    "documents/1749289610.jpg"
    "documents/1749289616.jpg"
    "documents/1749290786.jpg"
    "documents/1749302402.jpg"
    "documents/1749302415.jpg"
    "documents/1749309159.jpg"
    "documents/1750023183.jpg"
    "documents/1751543092.jpg"
    "documents/1751543478.jpg"
    "documents/1751546936.jpg"
    "documents/org_logo_1748535599.jpg"
    "documents/org_logo_1748535688.jpg"
    "documents/org_logo_1748535797.jpg"
    "documents/org_logo_1748538448.jpg"
    "documents/org_logo_1748540439.jpg"
    "documents/org_logo_1749126231.jpg"
    "documents/org_logo_1749126521.jpg"
    "documents/org_logo_1749126644.jpg"
    "documents/org_logo_1752322784.jpg"
    "documents/profile_1749127235.jpg"
    "documents/profile_1749127440.jpg"
    "documents/profile_1749127776.jpg"
    "documents/profile_1752660804.jpg"
    "documents/profile_1752663029.jpg"
    "documents/profile_1752673488.jpg"
    "documents/profile_1752712714.jpg"
    "documents/profile_1753264622.jpg"
    "images//sp_1.webp"
    "images//sp_10.webp"
    "images//sp_11.webp"
    "images//sp_12.webp"
    "images//sp_13.webp"
    "images//sp_14.webp"
    "images//sp_15.webp"
    "images//sp_16.webp"
    "images//sp_17.webp"
    "images//sp_18.webp"
    "images//sp_19.webp"
    "images//sp_2.webp"
    "images//sp_20.webp"
    "images//sp_3.webp"
    "images//sp_4.webp"
    "images//sp_5.webp"
    "images//sp_6.webp"
    "images//sp_7.webp"
    "images//sp_8.webp"
    "images//sp_9.webp"
    "profile_image//acharya_dr_sushila_ji.webp"
    "profile_image//acharya_monika_ji.webp"
    "profile_image//acharya_suman.webp"
    "profile_image//arya_dharmvir_shastri.webp"
    "profile_image//arya_jasbir_singh.webp"
    "profile_image//arya_mahasangh.webp"
    "profile_image//arya_renu.webp"
    "profile_image//arya_sandip_shastri.webp"
    "profile_image//arya_sukhvidra.webp"
    "profile_image//arya_sushil.webp"
    "profile_image//soumya_arya.webp"
    "profile_image/achary_hanumat_prasad.webp"
    "profile_image/achary_jitendra.webp"
    "profile_image/achary_loknath.webp"
    "profile_image/achary_mahesh.webp"
    "profile_image/achary_sanjiv.webp"
    "profile_image/achary_satish.webp"
    "profile_image/achary_varchaspati.webp"
    "profile_image/acharya_indra.webp"
    "profile_image/arya_shivnarayan.webp"
    "profile_image/arya_vedprakash.webp"
    "profile_image/dr_mahesh_arya.webp"
    "profile_image/upachary_jasbir_arya.webp"
)

success_count=0
fail_count=0
total_files=${#prod_files[@]}

echo "📊 Found $total_files files to migrate..."
echo ""

for file_path in "${prod_files[@]}"; do
    copy_and_fix_file "$file_path"
    if [[ $? -eq 0 ]]; then
        ((success_count++))
    else
        ((fail_count++))
    fi
done

echo ""
echo "🎉 PHASE 1 COMPLETE - FILE MIGRATION:"
echo "   📁 Total files: $total_files"
echo "   ✅ Successfully copied: $success_count"
echo "   ❌ Failed to copy: $fail_count"
echo ""

if [[ $fail_count -gt 10 ]]; then
    echo "⚠️ Too many file migration failures. Stopping to avoid database corruption."
    echo "   Please check staging storage accessibility and retry."
    exit 1
fi

echo "📋 PHASE 2: Updating database URLs (staging → prod)..."
echo "   🔄 This will update all database references to point to prod storage"
echo ""

# Use Supabase MCP to update database URLs - we'll do this after the script completes

echo "🎊 PRODUCTION MIGRATION SUMMARY:"
echo "   ✅ Files copied to prod storage: $success_count"
echo "   📋 Ready for database URL updates"
echo "   🔗 Example URL change:"
echo "      FROM: https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/documents/org_logo_1748535599.jpg"
echo "      TO:   https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/documents/org_logo_1748535599.jpg"
echo ""
echo "📋 NEXT STEPS:"
echo "   1. Files have been copied with correct MIME types"
echo "   2. Database URLs will be updated via Supabase MCP"
echo "   3. Production environment will be isolated from staging"
echo ""

if [[ $success_count -eq $total_files ]]; then
    echo "🎊 PHASE 1 SUCCESS! All files migrated successfully!"
    echo "🌟 Ready to update database URLs..."
    exit 0
else
    echo "⚠️ PHASE 1 PARTIAL SUCCESS: $fail_count files failed to migrate"
    echo "   You can still proceed with database updates for successful files"
    exit 1
fi
