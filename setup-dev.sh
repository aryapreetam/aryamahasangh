#!/bin/bash

# Development Setup Script for KMP-Secrets-Plugin
# This script helps set up the development environment with local.properties

echo "🚀 Setting up Arya Mahasangh development environment..."
echo ""

# Check if local.properties exists
if [ ! -f "local.properties" ]; then
    echo "📋 Creating local.properties from template..."
    if [ -f "local.properties.template" ]; then
        cp local.properties.template local.properties
        echo "✅ local.properties created from template!"
    else
        echo "⚠️  local.properties.template not found, creating basic local.properties..."
        cat > local.properties << 'EOF'
# Secrets Configuration for KMP-Secrets-Plugin
# This file is gitignored and should never be committed

# App version (semantic versioning)
app_version=1.0.0

# Development Configuration (use underscores for valid Kotlin identifiers)
dev_supabase_url=
dev_supabase_key=
dev_server_url=http://localhost:4000
dev_googlemaps_apikey=

# Production Configuration  
prod_supabase_url=
prod_supabase_key=
prod_server_url=https://your-production-server.com
prod_googlemaps_apikey=

# Current environment (dev or prod)
environment=dev

# GitHub Personal Access Token
github_pat=
EOF
        echo "✅ Basic local.properties created!"
    fi
    
    echo ""
    echo "⚠️  IMPORTANT: Please edit local.properties and add your actual configuration values:"
    echo "   - Supabase URL and API key (dev and prod)"
    echo "   - Server URLs for dev and prod"
    echo "   - Google Maps API key (if needed)"
    echo ""
    echo "📖 You can get these values from:"
    echo "   - Supabase Dashboard: https://app.supabase.com/"
    echo "   - Your server deployment"
    echo "   - Google Cloud Console (for Maps API)"
    echo ""
else
    echo "✅ local.properties already exists"
fi

# Check if local.properties has been configured (look for template placeholder)
if grep -q "your-production-server.com" local.properties 2>/dev/null; then
    echo "⚠️  WARNING: local.properties still contains template values!"
    echo "   Please update it with your actual configuration."
    echo ""
fi

# Check if KMP-Secrets-Plugin will work
echo "🔍 Checking KMP-Secrets-Plugin setup..."

# Check if the plugin is in build.gradle.kts
if grep -q "kmp-secrets-plugin" composeApp/build.gradle.kts 2>/dev/null; then
    echo "✅ KMP-Secrets-Plugin found in build.gradle.kts"
else
    echo "⚠️  KMP-Secrets-Plugin not found in composeApp/build.gradle.kts"
    echo "   Make sure the plugin is added to your build configuration"
fi

# Check if Secrets.kt will be generated
SECRETS_FILE="composeApp/src/commonMain/kotlin/secrets/Secrets.kt"
if [ -f "$SECRETS_FILE" ]; then
    echo "✅ Secrets.kt already generated"
else
    echo "📝 Secrets.kt will be generated on first build"
fi

echo ""
echo "🔧 Development setup complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Edit local.properties with your actual values"
echo "   2. Run: ./gradlew compileKotlinMetadata  (to generate Secrets.kt)"
echo "   3. Run: ./gradlew build"
echo "   4. Start developing!"
echo ""
echo "🎯 How the new system works:"
echo "   - KMP-Secrets-Plugin reads local.properties"
echo "   - Generates type-safe Secrets.kt object"
echo "   - Available in all platforms (Android, iOS, Desktop, Web)"
echo "   - Access via: Secrets.dev_supabase_url, AppConfig.supabaseUrl, etc."
echo ""
echo "🚀 Creating releases:"
echo "   - CI uses version from local.properties.template"
echo "   - Before pushing changes, update app_version in local.properties.template"
echo "   - If you forget, CI will auto-increment the patch version"
echo "   - Example: Change app_version=1.0.6 to app_version=1.0.7"
echo ""
echo "🔒 Security reminders:"
echo "   - local.properties is gitignored and will never be committed"
echo "   - Use environment variables in CI/production"
echo "   - Never commit actual secrets to version control"
echo "   - Generated Secrets.kt is also auto-gitignored"
