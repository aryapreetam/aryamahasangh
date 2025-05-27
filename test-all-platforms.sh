#!/bin/bash

# Test script to verify secrets loading on all platforms
# This script checks that all platform-specific secrets files are in place

set -e

echo "🧪 Testing Secrets Configuration for All Platforms"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if file exists and show preview
check_file() {
    local file_path="$1"
    local platform="$2"
    local description="$3"
    
    echo -e "\n${BLUE}📋 Checking $platform: $description${NC}"
    
    if [ -f "$file_path" ]; then
        echo -e "${GREEN}✅ Found: $file_path${NC}"
        
        # Show file size
        local size=$(wc -c < "$file_path")
        echo "   📏 File size: $size bytes"
        
        # Show preview (first 3 lines, masking sensitive data)
        echo "   📄 Preview:"
        head -3 "$file_path" | sed 's/key=.*/key=***/' | sed 's/^/   | /'
        
        # Count non-empty, non-comment lines
        local config_lines=$(grep -v '^#' "$file_path" | grep -v '^$' | wc -l)
        echo "   📊 Configuration entries: $config_lines"
        
        return 0
    else
        echo -e "${RED}❌ Missing: $file_path${NC}"
        return 1
    fi
}

# Function to check directory exists
check_directory() {
    local dir_path="$1"
    local description="$2"
    
    if [ -d "$dir_path" ]; then
        echo -e "${GREEN}✅ Directory exists: $dir_path${NC}"
        return 0
    else
        echo -e "${RED}❌ Directory missing: $dir_path${NC}"
        echo "   $description"
        return 1
    fi
}

# Track overall status
OVERALL_STATUS=0

echo -e "\n${BLUE}🔍 Phase 1: Checking Source Files${NC}"
echo "================================="

# Check main secrets file
if ! check_file "secrets.properties" "Main" "Root secrets configuration"; then
    echo -e "${YELLOW}⚠️  Run: cp secrets.properties.template secrets.properties${NC}"
    OVERALL_STATUS=1
fi

# Check template file
if ! check_file "secrets.properties.template" "Template" "Template for secrets"; then
    echo -e "${RED}❌ Template file missing - this should be in version control${NC}"
    OVERALL_STATUS=1
fi

echo -e "\n${BLUE}🔍 Phase 2: Checking Platform-Specific Files${NC}"
echo "============================================="

# Desktop (uses root file)
echo -e "\n${BLUE}🖥️  Desktop Platform${NC}"
echo "Uses secrets.properties from project root"
if [ -f "secrets.properties" ]; then
    echo -e "${GREEN}✅ Desktop configuration ready${NC}"
else
    echo -e "${RED}❌ Desktop configuration missing${NC}"
    OVERALL_STATUS=1
fi

# Android
echo -e "\n${BLUE}🤖 Android Platform${NC}"
if ! check_directory "composeApp/src/androidMain/assets" "Android assets directory"; then
    OVERALL_STATUS=1
fi

if ! check_file "composeApp/src/androidMain/assets/secrets.properties" "Android" "Assets secrets file"; then
    echo -e "${YELLOW}⚠️  Run: ./setup-secrets.sh to create Android assets file${NC}"
    OVERALL_STATUS=1
fi

# Web
echo -e "\n${BLUE}🌐 Web Platform${NC}"
if ! check_directory "composeApp/src/wasmJsMain/resources" "Web resources directory"; then
    OVERALL_STATUS=1
fi

if ! check_file "composeApp/src/wasmJsMain/resources/config.json" "Web" "Web configuration JSON"; then
    echo -e "${YELLOW}⚠️  Run: ./setup-secrets.sh to create Web config.json${NC}"
    OVERALL_STATUS=1
fi

# iOS
echo -e "\n${BLUE}🍎 iOS Platform${NC}"
if ! check_directory "iosApp/iosApp" "iOS app directory"; then
    OVERALL_STATUS=1
fi

IOS_FILES_OK=0
if ! check_file "iosApp/iosApp/secrets.properties" "iOS" "iOS bundle secrets file"; then
    echo -e "${YELLOW}⚠️  Run: ./setup-ios-secrets.sh to create iOS files${NC}"
    OVERALL_STATUS=1
    IOS_FILES_OK=1
fi

if ! check_file "iosApp/iosApp/config.json" "iOS" "iOS bundle config JSON"; then
    if [ $IOS_FILES_OK -eq 0 ]; then
        echo -e "${YELLOW}⚠️  Run: ./setup-ios-secrets.sh to create iOS files${NC}"
    fi
    OVERALL_STATUS=1
    IOS_FILES_OK=1
fi

if ! check_file "iosApp/iosApp/Config.swift" "iOS" "iOS Swift configuration"; then
    if [ $IOS_FILES_OK -eq 0 ]; then
        echo -e "${YELLOW}⚠️  Run: ./setup-ios-secrets.sh to create iOS files${NC}"
    fi
    OVERALL_STATUS=1
fi

echo -e "\n${BLUE}🔍 Phase 3: Checking Xcode Project Integration${NC}"
echo "=============================================="

if [ -f "iosApp/iosApp.xcodeproj/project.pbxproj" ]; then
    echo -e "${GREEN}✅ Xcode project file found${NC}"
    
    # Check if secrets files are referenced
    if grep -q "secrets.properties" "iosApp/iosApp.xcodeproj/project.pbxproj"; then
        echo -e "${GREEN}✅ secrets.properties referenced in Xcode project${NC}"
    else
        echo -e "${RED}❌ secrets.properties not referenced in Xcode project${NC}"
        echo -e "${YELLOW}⚠️  Run: ./add-ios-resources.sh to add files to Xcode project${NC}"
        OVERALL_STATUS=1
    fi
    
    if grep -q "config.json" "iosApp/iosApp.xcodeproj/project.pbxproj"; then
        echo -e "${GREEN}✅ config.json referenced in Xcode project${NC}"
    else
        echo -e "${RED}❌ config.json not referenced in Xcode project${NC}"
        OVERALL_STATUS=1
    fi
    
    # Check if files are in Resources build phase
    if grep -q "secrets.properties in Resources" "iosApp/iosApp.xcodeproj/project.pbxproj"; then
        echo -e "${GREEN}✅ secrets.properties in Resources build phase${NC}"
    else
        echo -e "${RED}❌ secrets.properties not in Resources build phase${NC}"
        OVERALL_STATUS=1
    fi
    
    if grep -q "config.json in Resources" "iosApp/iosApp.xcodeproj/project.pbxproj"; then
        echo -e "${GREEN}✅ config.json in Resources build phase${NC}"
    else
        echo -e "${RED}❌ config.json not in Resources build phase${NC}"
        OVERALL_STATUS=1
    fi
else
    echo -e "${RED}❌ Xcode project file not found${NC}"
    OVERALL_STATUS=1
fi

echo -e "\n${BLUE}🔍 Phase 4: Checking Build Files${NC}"
echo "================================"

# Check if build files exist (these are generated during build)
BUILD_DIRS=(
    "composeApp/build/processedResources/wasmJs/main"
    "composeApp/build/dist/wasmJs/developmentExecutable"
)

for build_dir in "${BUILD_DIRS[@]}"; do
    if [ -d "$build_dir" ]; then
        echo -e "${GREEN}✅ Build directory exists: $build_dir${NC}"
        if [ -f "$build_dir/config.json" ]; then
            echo -e "${GREEN}✅ Web config.json copied to build directory${NC}"
        fi
    else
        echo -e "${YELLOW}ℹ️  Build directory not found: $build_dir (will be created during build)${NC}"
    fi
done

echo -e "\n${BLUE}📋 Final Results${NC}"
echo "================"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}🎉 All platforms configured successfully!${NC}"
    echo ""
    echo -e "${GREEN}✅ Desktop: Uses secrets.properties from project root${NC}"
    echo -e "${GREEN}✅ Android: Uses secrets.properties from assets/${NC}"
    echo -e "${GREEN}✅ Web: Uses config.json from resources/${NC}"
    echo -e "${GREEN}✅ iOS: Uses secrets files from app bundle${NC}"
    echo ""
    echo -e "${BLUE}🚀 Ready to run on all platforms:${NC}"
    echo "   ./gradlew run                    # Desktop"
    echo "   ./gradlew assembleDebug          # Android"
    echo "   ./gradlew wasmJsBrowserRun       # Web"
    echo "   # iOS: Build in Xcode"
else
    echo -e "${RED}❌ Some platforms need configuration!${NC}"
    echo ""
    echo -e "${YELLOW}🔧 Quick fixes:${NC}"
    echo "   ./setup-secrets.sh               # Setup Android & Web"
    echo "   ./setup-ios-secrets.sh           # Complete iOS setup"
    echo "   cp secrets.properties.template secrets.properties  # Create main config"
    echo ""
    echo -e "${BLUE}📚 For detailed help, see SECRETS_SETUP_GUIDE.md${NC}"
fi

exit $OVERALL_STATUS