#!/bin/bash

# Version bump script for AryaMahasangh
# Usage: ./version-bump.sh [patch|minor|major]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if we're on the dev branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "dev" ]; then
    print_error "You must be on the 'dev' branch to create a release. Current branch: $CURRENT_BRANCH"
    exit 1
fi

# Check if working directory is clean
if ! git diff-index --quiet HEAD --; then
    print_error "Working directory is not clean. Please commit or stash your changes."
    exit 1
fi

# Get the bump type (default to patch)
BUMP_TYPE=${1:-patch}

if [[ "$BUMP_TYPE" != "patch" && "$BUMP_TYPE" != "minor" && "$BUMP_TYPE" != "major" ]]; then
    print_error "Invalid bump type. Use: patch, minor, or major"
    echo "Usage: $0 [patch|minor|major]"
    exit 1
fi

print_info "Bumping $BUMP_TYPE version..."

# Read current version from gradle.properties
if [ ! -f "gradle.properties" ]; then
    print_error "gradle.properties file not found!"
    exit 1
fi

CURRENT_VERSION=$(grep "^appVersion=" gradle.properties | cut -d'=' -f2 | tr -d ' ')

if [ -z "$CURRENT_VERSION" ]; then
    print_error "appVersion not found in gradle.properties!"
    exit 1
fi

print_info "Current version in gradle.properties: $CURRENT_VERSION"

# Parse version numbers
if [[ $CURRENT_VERSION =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    MAJOR=${BASH_REMATCH[1]}
    MINOR=${BASH_REMATCH[2]}
    PATCH=${BASH_REMATCH[3]}
    
    print_info "Parsed version: Major=$MAJOR, Minor=$MINOR, Patch=$PATCH"
    
    # Bump version based on type
    case $BUMP_TYPE in
        "major")
            NEW_MAJOR=$((MAJOR + 1))
            NEW_MINOR=0
            NEW_PATCH=0
            ;;
        "minor")
            NEW_MAJOR=$MAJOR
            NEW_MINOR=$((MINOR + 1))
            NEW_PATCH=0
            ;;
        "patch")
            NEW_MAJOR=$MAJOR
            NEW_MINOR=$MINOR
            NEW_PATCH=$((PATCH + 1))
            ;;
    esac
    
    NEW_VERSION="${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}"
else
    print_error "Current version '$CURRENT_VERSION' doesn't follow semantic versioning (x.y.z format)!"
    exit 1
fi

NEW_TAG="v${NEW_VERSION}"

print_info "New version will be: $NEW_VERSION (tag: $NEW_TAG)"

# Confirm with user
echo
read -p "🤔 Do you want to update version to $NEW_VERSION? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "Version bump cancelled."
    exit 0
fi

# Update gradle.properties
print_info "Updating gradle.properties..."
sed -i.bak "s/^appVersion=.*/appVersion=$NEW_VERSION/" gradle.properties
rm gradle.properties.bak

# Update iOS VersionInfo (if it exists and uses hardcoded values)
IOS_VERSION_FILE="composeApp/src/iosMain/kotlin/com/aryamahasangh/util/VersionInfo.ios.kt"
if [ -f "$IOS_VERSION_FILE" ]; then
    print_info "Updating iOS VersionInfo.ios.kt..."
    # Calculate version code (major*10000 + minor*100 + patch)
    NEW_VERSION_CODE=$((NEW_MAJOR * 10000 + NEW_MINOR * 100 + NEW_PATCH))
    
    # Update version name
    sed -i.bak "s/return \"[0-9]*\.[0-9]*\.[0-9]*\"/return \"$NEW_VERSION\"/" "$IOS_VERSION_FILE"
    # Update version code
    sed -i.bak "s/return [0-9]* \/\/ Version code for [0-9]*\.[0-9]*\.[0-9]*/return $NEW_VERSION_CODE \/\/ Version code for $NEW_VERSION/" "$IOS_VERSION_FILE"
    
    rm "${IOS_VERSION_FILE}.bak"
fi

# Commit the version changes
print_info "Committing version update..."
git add gradle.properties
if [ -f "$IOS_VERSION_FILE" ]; then
    git add "$IOS_VERSION_FILE"
fi
git commit -m "Bump version to $NEW_VERSION"

# Create and push the tag
print_info "Creating tag $NEW_TAG..."
git tag -a "$NEW_TAG" -m "Release $NEW_TAG"

print_info "Pushing changes and tag to origin..."
git push origin dev
git push origin "$NEW_TAG"

print_success "Version bumped to $NEW_VERSION!"
print_info "GitHub Actions will now create a release with this version."
print_info "Monitor the build at: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^.]*\).*/\1/')/actions"

echo
print_info "📝 Updated files:"
print_info "  - gradle.properties: appVersion=$NEW_VERSION"
if [ -f "$IOS_VERSION_FILE" ]; then
    print_info "  - iOS VersionInfo: $NEW_VERSION (code: $NEW_VERSION_CODE)"
fi

echo
print_info "🎉 Release $NEW_TAG is on its way!"
