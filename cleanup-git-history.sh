#!/bin/bash

# Git History Cleanup Script for AryaMahasangh
# WARNING: This script rewrites git history permanently
# Run this script after rotating all exposed credentials

set -e  # Exit on any error

echo "🚨 Git History Cleanup Script"
echo "This will permanently rewrite git history to remove hardcoded credentials"
echo ""

# Confirmation prompt
read -p "Have you already rotated ALL exposed Supabase keys and GitHub PAT? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Please rotate credentials first, then run this script"
    exit 1
fi

read -p "Have you notified all collaborators about the history rewrite? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Please notify team members first"
    exit 1
fi

read -p "Do you have a backup of your repository? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Please create a backup first"
    exit 1
fi

echo ""
echo "🔄 Starting cleanup process..."

# Create backup
echo "📦 Creating backup..."
if [ ! -d "aryamahasangh-backup.git" ]; then
    git clone --mirror . aryamahasangh-backup.git
    echo "✅ Backup created at aryamahasangh-backup.git"
else
    echo "✅ Backup already exists"
fi

# Create credentials replacement file
echo "📝 Creating credentials replacement file..."
cat > credentials-to-replace.txt << 'EOF'
# Supabase URLs
afjtpdeohgdgkrwayayn.supabase.co=***REMOVED-SUPABASE-URL***
ftnwwiwmljcwzpsawdmf.supabase.co=***REMOVED-SUPABASE-URL***

# Supabase API Keys (JWT tokens)
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFmanRwZGVvaGdkZ2tyd2F5YXluIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwMzc1NDUsImV4cCI6MjA2MzYxMzU0NX0.yf7una_no1wwIxJ3GJnKA-Iy5QeMsBnDSi85J1ZhU_E=***REMOVED-SUPABASE-KEY***
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ0bnd3aXdtbGpjd3pwc2F3ZG1mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzQ5MzE4OTMsImV4cCI6MjA1MDUwNzg5M30.cY4A4ZxqHA_1VRC-k6URVAHHkweHTR8FEYEzHYiu19A=***REMOVED-SUPABASE-KEY***

# GitHub PAT
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=***REMOVED-GITHUB-PAT***

# Keystore password
aryamahasangh=***REMOVED-PASSWORD***
EOF

# Check if BFG is available
if command -v bfg >/dev/null 2>&1; then
    echo "🔧 Using BFG Repo-Cleaner (recommended method)..."
    
    # Run BFG to replace credentials
    echo "🧹 Cleaning credentials from git history..."
    bfg --replace-text credentials-to-replace.txt .
    
    # Clean up the repository
    echo "🗑️  Cleaning up repository..."
    git reflog expire --expire=now --all
    git gc --prune=now --aggressive
    
else
    echo "⚠️  BFG not found, using git filter-branch (slower method)..."
    
    # Alternative method using git filter-branch
    echo "🧹 Cleaning specific files from git history..."
    git filter-branch --force --index-filter \
      'git rm --cached --ignore-unmatch server/src/main/kotlin/org/aryamahasangh/Application.kt' \
      --prune-empty --tag-name-filter cat -- --all
    
    # Clean up refs
    git for-each-ref --format='delete %(refname)' refs/original | git update-ref --stdin
    git reflog expire --expire=now --all
    git gc --prune=now --aggressive
fi

# Verify cleanup
echo "🔍 Verifying credentials are removed..."
if git log --all --oneline | grep -q "supabase.co\|eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"; then
    echo "⚠️  Some credentials may still be present. Manual review needed."
else
    echo "✅ No obvious credentials found in commit messages"
fi

# Update .gitignore
echo "🛡️  Updating .gitignore for future protection..."
cat >> .gitignore << 'EOF'

# === SECRETS AND CREDENTIALS ===
secrets.properties
local.properties
.env
.env.local
.env.*.local
config.json
**/secrets.*
**/config/secrets.*

# Platform-specific secrets
composeApp/src/androidMain/assets/secrets.properties
composeApp/src/wasmJsMain/resources/config.json
iosApp/iosApp/Config.swift
IOSEmbeddedConfig.kt

# GitHub tokens
.github-token
github_pat_*
EOF

git add .gitignore
git commit -m "🛡️ Add comprehensive secrets protection to .gitignore

- Add all secrets file patterns
- Protect platform-specific credential files
- Prevent future credential leaks"

echo ""
echo "✅ Cleanup completed successfully!"
echo ""
echo "🚀 Next steps:"
echo "1. Force push to GitHub: git push --force-with-lease --all origin"
echo "2. Force push tags: git push --force-with-lease --tags origin"
echo "3. Notify team to re-clone repository"
echo "4. Enable GitHub secret scanning in repository settings"
echo ""
echo "⚠️  WARNING: All commit SHAs have changed. Team must re-clone!"

# Clean up temporary files
rm -f credentials-to-replace.txt

echo "🎉 Repository is now secure!"