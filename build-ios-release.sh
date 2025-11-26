#!/bin/bash
set -e

echo "🏗️  iOS RELEASE BUILD PIPELINE"
echo "=============================="

# 1. Clean previous builds
echo "1️⃣  Cleaning previous builds..."
rm -rf build/ios/AryaMahasangh.xcarchive
rm -rf build/ios/export
# Don't clean Gradle - incremental builds are faster
# ./gradlew clean

# 2. Build Kotlin framework (secrets will be auto-generated)
echo "2️⃣  Building Kotlin framework..."
export SDK_NAME=iphoneos ARCHS=arm64 PLATFORM_NAME=iphoneos CONFIGURATION=Release
# Use --parallel for faster builds
./gradlew :composeApp:linkReleaseFrameworkIosArm64 --parallel
echo "✅ Framework built successfully"

# 3. Copy framework
echo "3️⃣  Copying framework..."
mkdir -p composeApp/build/xcode-frameworks/Release/iphoneos
cp -R composeApp/build/bin/iosArm64/releaseFramework/ComposeApp.framework \
  composeApp/build/xcode-frameworks/Release/iphoneos/
echo "✅ Framework copied successfully"

# 4. Create archive
echo "4️⃣  Creating Xcode archive..."
export OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath build/ios/AryaMahasangh.xcarchive \
  -jobs 1 archive
echo "✅ Archive created successfully"

# exportOptionsPlist step

# 5. Export IPA
echo "5️⃣  Exporting IPA..."
xcodebuild -exportArchive \
  -archivePath build/ios/AryaMahasangh.xcarchive \
  -exportOptionsPlist build/ios/ExportOptions.plist \
  -exportPath build/ios/export
echo "✅ IPA exported successfully"

# 6. Verify signing
echo "6️⃣  Verifying code signing..."
unzip -q build/ios/export/AryaMahasangh.ipa -d /tmp/ipa_verify
codesign -dvvv /tmp/ipa_verify/Payload/AryaMahasangh.app 2>&1 | grep "Authority=Apple Distribution" && \
  echo "✅ IPA properly signed for App Store" || \
  echo "⚠️  Warning: Signing verification inconclusive"
rm -rf /tmp/ipa_verify

echo ""
echo "✅ BUILD COMPLETE!"
echo "📦 IPA location: build/ios/export/AryaMahasangh.ipa"
echo ""
echo "🚀 Next steps:"
echo "   1. Upload via Transporter.app"
echo "   2. OR use: xcrun altool --upload-app -f build/ios/export/AryaMahasangh.ipa -t ios -u YOUR_APPLE_ID"
