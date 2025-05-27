#!/bin/bash

# Script to add secrets files to iOS Xcode project as bundle resources
# This ensures the files are included in the iOS app bundle

set -e

PROJECT_FILE="iosApp/iosApp.xcodeproj/project.pbxproj"
IOS_APP_DIR="iosApp/iosApp"

echo "🍎 Adding secrets files to iOS Xcode project..."

if [ ! -f "$PROJECT_FILE" ]; then
    echo "❌ Xcode project file not found: $PROJECT_FILE"
    exit 1
fi

# Generate unique IDs for the new file references (using timestamp and random)
TIMESTAMP=$(date +%s)
RANDOM_SUFFIX=$(od -An -N4 -tx4 /dev/urandom | tr -d ' ')

SECRETS_PROPS_ID="SEC${TIMESTAMP}${RANDOM_SUFFIX:0:16}"
CONFIG_JSON_ID="CFG${TIMESTAMP}${RANDOM_SUFFIX:0:16}"
CONFIG_SWIFT_ID="SWF${TIMESTAMP}${RANDOM_SUFFIX:0:16}"

SECRETS_PROPS_BUILD_ID="SECB${TIMESTAMP}${RANDOM_SUFFIX:0:15}"
CONFIG_JSON_BUILD_ID="CFGB${TIMESTAMP}${RANDOM_SUFFIX:0:15}"
CONFIG_SWIFT_BUILD_ID="SWFB${TIMESTAMP}${RANDOM_SUFFIX:0:15}"

echo "📋 Generated IDs for new file references..."

# Backup the project file
cp "$PROJECT_FILE" "$PROJECT_FILE.backup"

# Add file references to PBXFileReference section
echo "📋 Adding file references..."

# Find the end of PBXFileReference section and add our files
sed -i.tmp '/^\/\* End PBXFileReference section \*\//i\
		'$CONFIG_JSON_ID' /* config.json */ = {isa = PBXFileReference; fileEncoding = 4; lastKnownFileType = text.json; path = config.json; sourceTree = "<group>"; };\
		'$CONFIG_SWIFT_ID' /* Config.swift */ = {isa = PBXFileReference; fileEncoding = 4; lastKnownFileType = sourcecode.swift; path = Config.swift; sourceTree = "<group>"; };\
		'$SECRETS_PROPS_ID' /* secrets.properties */ = {isa = PBXFileReference; fileEncoding = 4; lastKnownFileType = text; path = secrets.properties; sourceTree = "<group>"; };' "$PROJECT_FILE"

# Add build file references to PBXBuildFile section
echo "📋 Adding build file references..."

sed -i.tmp2 '/^\/\* End PBXBuildFile section \*\//i\
		'$CONFIG_JSON_BUILD_ID' /* config.json in Resources */ = {isa = PBXBuildFile; fileRef = '$CONFIG_JSON_ID' /* config.json */; };\
		'$CONFIG_SWIFT_BUILD_ID' /* Config.swift in Sources */ = {isa = PBXBuildFile; fileRef = '$CONFIG_SWIFT_ID' /* Config.swift */; };\
		'$SECRETS_PROPS_BUILD_ID' /* secrets.properties in Resources */ = {isa = PBXBuildFile; fileRef = '$SECRETS_PROPS_ID' /* secrets.properties */; };' "$PROJECT_FILE"

# Add files to the iosApp group (find the group that contains other app files)
echo "📋 Adding files to iosApp group..."

# Find the iosApp group and add our files
sed -i.tmp3 '/7555FF82242A565900829871 \/\* ContentView.swift \*\//a\
				'$CONFIG_JSON_ID' /* config.json */,\
				'$CONFIG_SWIFT_ID' /* Config.swift */,\
				'$SECRETS_PROPS_ID' /* secrets.properties */,' "$PROJECT_FILE"

# Add files to Resources build phase
echo "📋 Adding files to Resources build phase..."

sed -i.tmp4 '/058557BB273AAA24004C7B11 \/\* Assets.xcassets in Resources \*\//a\
				'$CONFIG_JSON_BUILD_ID' /* config.json in Resources */,\
				'$SECRETS_PROPS_BUILD_ID' /* secrets.properties in Resources */,' "$PROJECT_FILE"

# Add Config.swift to Sources build phase
echo "📋 Adding Config.swift to Sources build phase..."

sed -i.tmp5 '/7555FF83242A565900829871 \/\* ContentView.swift in Sources \*\//a\
				'$CONFIG_SWIFT_BUILD_ID' /* Config.swift in Sources */,' "$PROJECT_FILE"

# Clean up temporary files
rm -f "$PROJECT_FILE.tmp" "$PROJECT_FILE.tmp2" "$PROJECT_FILE.tmp3" "$PROJECT_FILE.tmp4" "$PROJECT_FILE.tmp5"

echo "✅ Successfully added secrets files to iOS Xcode project!"
echo ""
echo "📋 Added files:"
echo "   - secrets.properties (as bundle resource)"
echo "   - config.json (as bundle resource)"
echo "   - Config.swift (as source file)"
echo ""
echo "🚀 The files will now be included in the iOS app bundle when you build the project."
echo ""
echo "⚠️  Note: If you encounter any issues, you can restore the backup:"
echo "   cp $PROJECT_FILE.backup $PROJECT_FILE"