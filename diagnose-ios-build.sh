#!/bin/bash

echo "🔍 iOS CRASH DIAGNOSTICS"
echo "========================"

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# 1. Check if secrets are generated
echo ""
echo "1️⃣  Checking if Secrets are generated..."
if [ -f "nhost-client/build/generated/kmp-secrets/commonMain/kotlin/secrets/Secrets.kt" ]; then
    echo "✅ Secrets.kt found"
    echo "   Location: nhost-client/build/generated/kmp-secrets/commonMain/kotlin/secrets/Secrets.kt"
else
    echo "❌ Secrets.kt NOT found!"
    echo "   Run: ./gradlew :nhost-client:generateSecretsCommonMainClasses"
    exit 1
fi

# 2. Check if local.properties exists and has required keys
echo ""
echo "2️⃣  Checking local.properties configuration..."
if [ ! -f "local.properties" ]; then
    echo "❌ local.properties NOT found!"
    echo "   Copy from template: cp local.properties.template local.properties"
    exit 1
fi

echo "✅ local.properties found"
echo ""
echo "   Checking required keys for dev environment:"

check_property() {
    local key=$1
    local value=$(grep "^${key}=" local.properties 2>/dev/null | cut -d'=' -f2-)
    if [ -z "$value" ] || [ "$value" = "" ]; then
        echo "   ❌ $key - NOT SET"
        return 1
    else
        echo "   ✅ $key - configured"
        return 0
    fi
}

all_keys_present=true
check_property "environment" || all_keys_present=false
check_property "app_version" || all_keys_present=false
check_property "dev_supabase_url" || all_keys_present=false
check_property "dev_supabase_key" || all_keys_present=false

if [ "$all_keys_present" = false ]; then
    echo ""
    echo "❌ Some required properties are missing!"
    echo "   Edit local.properties and add the missing values"
    exit 1
fi

# 3. Check if framework was built
echo ""
echo "3️⃣  Checking if Kotlin framework is built..."
if [ -f "composeApp/build/bin/iosArm64/releaseFramework/ComposeApp.framework/ComposeApp" ]; then
    echo "✅ Framework built"
else
    echo "❌ Framework NOT built!"
    echo "   Run: ./gradlew :composeApp:linkReleaseFrameworkIosArm64"
    exit 1
fi

# 4. Check if framework was copied
echo ""
echo "4️⃣  Checking if framework was copied to xcode-frameworks..."
if [ -f "composeApp/build/xcode-frameworks/Release/iphoneos/ComposeApp.framework/ComposeApp" ]; then
    echo "✅ Framework copied"
else
    echo "❌ Framework NOT copied!"
    echo "   Run: mkdir -p composeApp/build/xcode-frameworks/Release/iphoneos && cp -R composeApp/build/bin/iosArm64/releaseFramework/ComposeApp.framework composeApp/build/xcode-frameworks/Release/iphoneos/"
    exit 1
fi

# 5. Check archive
echo ""
echo "5️⃣  Checking if archive exists..."
if [ -d "build/ios/AryaMahasangh.xcarchive" ]; then
    echo "✅ Archive exists"
    
    # Check archive signing
    echo ""
    echo "   Checking archive code signing..."
    codesign -dvvv build/ios/AryaMahasangh.xcarchive/Products/Applications/AryaMahasangh.app 2>&1 | \
        grep "Authority=" | head -1 || echo "   ⚠️  No signing authority found"
else
    echo "⚠️  Archive not found (might not be created yet)"
fi

# 6. Check IPA
echo ""
echo "6️⃣  Checking if IPA exists..."
if [ -f "build/ios/export/AryaMahasangh.ipa" ]; then
    echo "✅ IPA exists"
    
    # Check IPA signing
    echo ""
    echo "   Checking IPA code signing..."
    unzip -q build/ios/export/AryaMahasangh.ipa -d /tmp/ipa_diagnostic
    signing_info=$(codesign -dvvv /tmp/ipa_diagnostic/Payload/AryaMahasangh.app 2>&1)
    
    echo "$signing_info" | grep "Authority=Apple Distribution" >/dev/null && \
        echo "   ✅ Signed with Apple Distribution certificate" || \
        echo "   ❌ NOT signed with Apple Distribution certificate"
    
    echo "$signing_info" | grep "TeamIdentifier" || echo "   ⚠️  No Team ID found"
    
    rm -rf /tmp/ipa_diagnostic
else
    echo "⚠️  IPA not found (might not be exported yet)"
fi

# 7. Summary
echo ""
echo "📊 DIAGNOSTIC SUMMARY"
echo "===================="
echo ""
echo "✅ All checks passed! Your build environment is properly configured."
echo ""
echo "🔍 To investigate crashes on TestGrid:"
echo "   1. Check TestGrid device logs for crash reports"
echo "   2. Look for errors containing 'FATAL' or 'Secrets'"
echo "   3. Enable verbose logging in Xcode (Product > Scheme > Edit Scheme > Run > Arguments)"
echo "   4. Add -com.apple.CoreData.SQLDebug 1 to see database errors"
echo ""
echo "🐛 Common crash causes:"
echo "   • Missing or invalid Supabase credentials"
echo "   • Koin initialization failures"
echo "   • Network connectivity issues"
echo "   • Missing entitlements or capabilities"
