#!/bin/bash

# Script to migrate package name from org.aryamahasangh to com.aryamahasangh
# This script will:
# 1. Create new directory structure (com/aryamahasangh)
# 2. Move all files from org/aryamahasangh to com/aryamahasangh
# 3. Update package declarations in all files
# 4. Update import statements in all files

echo "🚀 Starting package name migration from org.aryamahasangh to com.aryamahasangh..."

# Find all source directories that contain org/aryamahasangh
SOURCE_DIRS=(
  "composeApp/src"
  "server/src"
  "shared/src"
  "ui-components/src"
  "ui-components-gallery/src"
)

for SOURCE_DIR in "${SOURCE_DIRS[@]}"; do
  if [ -d "$SOURCE_DIR" ]; then
    echo "📁 Processing directory: $SOURCE_DIR"
    
    # Find all org/aryamahasangh directories
    find "$SOURCE_DIR" -type d -path "*/org/aryamahasangh" | while read -r ORG_DIR; do
      echo "  🔄 Moving: $ORG_DIR"
      
      # Get the parent directory (should contain 'org')
      PARENT_DIR=$(dirname "$ORG_DIR")
      
      # Create com directory if it doesn't exist
      COM_DIR="$PARENT_DIR/com"
      mkdir -p "$COM_DIR"
      
      # Move aryamahasangh directory from org to com
      ARYAMAHASANGH_DIR="$ORG_DIR"
      NEW_ARYAMAHASANGH_DIR="$COM_DIR/aryamahasangh"
      
      if [ -d "$ARYAMAHASANGH_DIR" ]; then
        mv "$ARYAMAHASANGH_DIR" "$NEW_ARYAMAHASANGH_DIR"
        echo "    ✅ Moved to: $NEW_ARYAMAHASANGH_DIR"
        
        # Remove empty org directory if it exists
        if [ -d "$PARENT_DIR/org" ] && [ -z "$(ls -A "$PARENT_DIR/org")" ]; then
          rmdir "$PARENT_DIR/org"
          echo "    🗑️  Removed empty org directory"
        fi
      fi
    done
  fi
done

echo ""
echo "📝 Updating package declarations in all Kotlin files..."

# Update package declarations in all Kotlin files
find . -name "*.kt" -type f | while read -r FILE; do
  if grep -q "^package org\.aryamahasangh" "$FILE"; then
    echo "  📦 Updating package in: $FILE"
    sed -i.bak 's/^package org\.aryamahasangh/package com.aryamahasangh/g' "$FILE"
    rm "$FILE.bak" 2>/dev/null || true
  fi
done

echo ""
echo "📥 Updating import statements in all Kotlin files..."

# Update import statements in all Kotlin files
find . -name "*.kt" -type f | while read -r FILE; do
  if grep -q "import org\.aryamahasangh" "$FILE"; then
    echo "  📥 Updating imports in: $FILE"
    sed -i.bak 's/import org\.aryamahasangh/import com.aryamahasangh/g' "$FILE"
    rm "$FILE.bak" 2>/dev/null || true
  fi
done

echo ""
echo "🧹 Cleaning up any remaining org.aryamahasangh references..."

# Update any remaining references in other file types
find . -name "*.json" -o -name "*.xml" -o -name "*.md" | while read -r FILE; do
  if grep -q "org\.aryamahasangh" "$FILE"; then
    echo "  🔧 Updating references in: $FILE"
    sed -i.bak 's/org\.aryamahasangh/com.aryamahasangh/g' "$FILE"
    rm "$FILE.bak" 2>/dev/null || true
  fi
done

echo ""
echo "✅ Package name migration completed!"
echo ""
echo "📋 Next steps:"
echo "1. Clean and rebuild the project: ./gradlew clean build"
echo "2. Regenerate Apollo GraphQL sources"
echo "3. Test the application on all platforms"
echo "4. Update any Supabase configurations if needed"
echo ""
echo "⚠️  Remember to:"
echo "- Update any CI/CD configurations"
echo "- Update deep link configurations"
echo "- Update any external service configurations"
echo "- Check for any hardcoded package references"
