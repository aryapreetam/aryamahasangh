#!/bin/bash

# Complete iOS secrets setup script
# This script handles all aspects of iOS secrets configuration

set -e

echo "🍎 Complete iOS Secrets Setup"
echo "=============================="

# Step 1: Run the main secrets setup
echo "📋 Step 1: Running main secrets setup..."
./setup-secrets.sh

# Step 2: Add files to Xcode project
echo ""
echo "📋 Step 2: Adding files to Xcode project..."
./add-ios-resources.sh

# Step 3: Verify files exist
echo ""
echo "📋 Step 3: Verifying iOS files..."

IOS_APP_DIR="iosApp/iosApp"
FILES_TO_CHECK=(
    "$IOS_APP_DIR/secrets.properties"
    "$IOS_APP_DIR/config.json"
    "$IOS_APP_DIR/Config.swift"
)

ALL_FILES_EXIST=true

for file in "${FILES_TO_CHECK[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ Found: $file"
        # Show first few lines for verification
        echo "   Preview:"
        head -3 "$file" | sed 's/^/   | /'
    else
        echo "❌ Missing: $file"
        ALL_FILES_EXIST=false
    fi
done

# Step 4: Verify Xcode project integration
echo ""
echo "📋 Step 4: Verifying Xcode project integration..."

PROJECT_FILE="iosApp/iosApp.xcodeproj/project.pbxproj"
if grep -q "secrets.properties" "$PROJECT_FILE" && grep -q "config.json" "$PROJECT_FILE"; then
    echo "✅ Files are referenced in Xcode project"
    
    # Check if they're in Resources build phase
    if grep -q "secrets.properties in Resources" "$PROJECT_FILE" && grep -q "config.json in Resources" "$PROJECT_FILE"; then
        echo "✅ Files are included in Resources build phase"
    else
        echo "⚠️  Files found in project but may not be in Resources build phase"
    fi
else
    echo "❌ Files are not properly referenced in Xcode project"
    ALL_FILES_EXIST=false
fi

# Step 5: Final status
echo ""
echo "📋 Final Status:"
echo "================"

if [ "$ALL_FILES_EXIST" = true ]; then
    echo "🎉 iOS secrets setup completed successfully!"
    echo ""
    echo "✅ All files are in place:"
    echo "   - secrets.properties (bundle resource)"
    echo "   - config.json (bundle resource)"  
    echo "   - Config.swift (source file)"
    echo ""
    echo "✅ Files are properly integrated into Xcode project"
    echo ""
    echo "🚀 Next steps:"
    echo "   1. Build your iOS app in Xcode"
    echo "   2. The secrets should now load properly from the bundle"
    echo "   3. Check the console output for confirmation"
    echo ""
    echo "🔍 Expected console output:"
    echo "   ✅ Loaded secrets from iOS bundle"
    echo "   ✅ Configuration initialized successfully"
    echo "   📋 Current configuration: [your actual values]"
else
    echo "❌ iOS secrets setup encountered issues!"
    echo ""
    echo "🔧 Troubleshooting steps:"
    echo "   1. Ensure you have the secrets.properties file in project root"
    echo "   2. Run ./setup-secrets.sh to regenerate files"
    echo "   3. Run ./add-ios-resources.sh to add files to Xcode project"
    echo "   4. Open iosApp.xcodeproj in Xcode and verify files are visible"
    echo "   5. Clean and rebuild the iOS project"
    echo ""
    echo "📞 If issues persist, check the iOS SecretsLoader debug output"
fi

echo ""
echo "📚 For more information, see SECRETS_SETUP_GUIDE.md"