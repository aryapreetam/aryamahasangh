#!/usr/bin/env bash
# Archive & export IPA for AryaMahasangh iOS (Kotlin Multiplatform) from CLI.
# Usage examples:
#   ./scripts/ios-archive.sh -v 1.0.16 -b 16
#   ./scripts/ios-archive.sh -v 1.0.16 -b 16 -u you@example.com -p app-specific-password
#   ./scripts/ios-archive.sh -v 1.0.16 -b 16 --api-key KEYID --api-issuer ISSUERID

set -euo pipefail

# --- Config (can be parameterized later) ---
APP_NAME="AryaMahasangh"
SCHEME="iosApp"
PROJECT="iosApp/iosApp.xcodeproj"
BUNDLE_ID="com.aryamahasangh"
TEAM_ID="ZKAG3ZRSRL"
CONFIGURATION="Release"
DESTINATION="generic/platform=iOS"

ARCHIVE_DIR="build/ios"
ARCHIVE_PATH="$ARCHIVE_DIR/${APP_NAME}.xcarchive"
EXPORT_DIR="$ARCHIVE_DIR/export"
EXPORT_OPTIONS_PLIST="$ARCHIVE_DIR/ExportOptions.plist"

# --- Inputs ---
VERSION=""       # CFBundleShortVersionString
BUILD_NUMBER=""  # CFBundleVersion (must increment each upload)
APPLE_ID=""
APP_SPECIFIC_PW=""
API_KEY=""
API_ISSUER=""
UPLOAD_METHOD="altool"  # altool | transporter | api

while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version) VERSION="$2"; shift 2;;
    -b|--build) BUILD_NUMBER="$2"; shift 2;;
    -u|--apple-id) APPLE_ID="$2"; shift 2;;
    -p|--app-password) APP_SPECIFIC_PW="$2"; shift 2;;
    --api-key) API_KEY="$2"; shift 2;;
    --api-issuer) API_ISSUER="$2"; shift 2;;
    --upload-method) UPLOAD_METHOD="$2"; shift 2;;
    *) echo "Unknown arg: $1"; exit 1;;
  esac
done

mkdir -p "$ARCHIVE_DIR"

# --- Optional version bump in Info.plist ---
if [[ -n "$VERSION" ]]; then
  echo "Setting marketing version to $VERSION"
  /usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $VERSION" iosApp/iosApp/Info.plist
fi
if [[ -n "$BUILD_NUMBER" ]]; then
  echo "Setting build number to $BUILD_NUMBER"
  /usr/libexec/PlistBuddy -c "Set :CFBundleVersion $BUILD_NUMBER" iosApp/iosApp/Info.plist
fi

# --- Prebuild Kotlin frameworks to reduce xcodebuild work ---
# Compose/iOS Gradle tasks need Xcode env to infer archs; provide minimal vars.
export SDK_NAME=iphoneos
export ARCHS=arm64
export PLATFORM_NAME=iphoneos
export CONFIGURATION

echo "Pre-building Kotlin frameworks (SDK_NAME=$SDK_NAME ARCHS=$ARCHS PLATFORM_NAME=$PLATFORM_NAME CONFIGURATION=$CONFIGURATION) ..."
if ! ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode -Pconfiguration=$CONFIGURATION; then
  echo "embedAndSignAppleFrameworkForXcode failed outside Xcode; trying device framework task as fallback..."
  ./gradlew :composeApp:linkReleaseFrameworkIosArm64 || {
    echo "Gradle prebuild fallback failed"; exit 1;
  }
fi

# Make Xcode build phase skip Gradle (to avoid duplicate invocation)
export OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES

# --- Archive with xcodebuild ---
echo "Cleaning DerivedData to reduce memory usage..."
rm -rf ~/Library/Developer/Xcode/DerivedData/*

echo "Archiving with xcodebuild (scheme=$SCHEME, config=$CONFIGURATION)..."
if command -v xcpretty >/dev/null 2>&1; then
  xcodebuild \
    -project "$PROJECT" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -destination "$DESTINATION" \
    -archivePath "$ARCHIVE_PATH" \
    -jobs 1 \
    clean archive | xcpretty
else
  xcodebuild \
    -project "$PROJECT" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -destination "$DESTINATION" \
    -archivePath "$ARCHIVE_PATH" \
    -jobs 1 \
    clean archive
fi

if [[ ! -d "$ARCHIVE_PATH" ]]; then
  echo "Archive not found at $ARCHIVE_PATH"; exit 1;
fi

# --- ExportOptions.plist ---
cat > "$EXPORT_OPTIONS_PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key><string>app-store</string>
  <key>teamID</key><string>${TEAM_ID}</string>
  <key>compileBitcode</key><true/>
  <key>destination</key><string>export</string>
  <key>stripSwiftSymbols</key><true/>
  <key>signingStyle</key><string>automatic</string>
  <key>manageAppVersionAndBuildNumber</key><false/>
  <key>uploadBitcode</key><true/>
  <key>generateAppStoreInformation</key><false/>
</dict>
</plist>
EOF

mkdir -p "$EXPORT_DIR"

echo "Exporting IPA..."
if command -v xcpretty >/dev/null 2>&1; then
  xcodebuild -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportOptionsPlist "$EXPORT_OPTIONS_PLIST" \
    -exportPath "$EXPORT_DIR" | xcpretty
else
  xcodebuild -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportOptionsPlist "$EXPORT_OPTIONS_PLIST" \
    -exportPath "$EXPORT_DIR"
fi

IPA_PATH=$(find "$EXPORT_DIR" -name "*.ipa" | head -1)
if [[ -z "$IPA_PATH" ]]; then
  echo "IPA not produced"; exit 1;
fi

echo "IPA exported: $IPA_PATH"

echo "Upload method: $UPLOAD_METHOD"
case "$UPLOAD_METHOD" in
  altool)
    if [[ -n "$API_KEY" && -n "$API_ISSUER" ]]; then
      echo "Uploading via App Store Connect API key (altool)..."
      xcrun altool --upload-app -f "$IPA_PATH" -t ios --apiKey "$API_KEY" --apiIssuer "$API_ISSUER"
    elif [[ -n "$APPLE_ID" && -n "$APP_SPECIFIC_PW" ]]; then
      echo "Uploading via Apple ID (altool)..."
      xcrun altool --upload-app -f "$IPA_PATH" -t ios -u "$APPLE_ID" -p "$APP_SPECIFIC_PW"
    else
      echo "Skipping upload: provide --api-key/--api-issuer OR -u/-p"
    fi
    ;;
  transporter)
    if [[ -n "$APPLE_ID" && -n "$APP_SPECIFIC_PW" ]]; then
      echo "Uploading via Transporter..."
      /Applications/Transporter.app/Contents/itms/bin/transporter -m upload -u "$APPLE_ID" -p "$APP_SPECIFIC_PW" -f "$IPA_PATH" -primaryBundleId "$BUNDLE_ID" || true
    else
      echo "Transporter requires -u and -p"
    fi
    ;;
  api)
    echo "API upload mode placeholder (use App Store Connect API)."
    ;;
  *)
    echo "Unknown upload method: $UPLOAD_METHOD"
    ;;
esac

echo "Done."
