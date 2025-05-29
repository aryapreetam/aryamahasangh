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

# Get the latest semantic version tag
LATEST_TAG=$(git tag -l "v[0-9]*.[0-9]*.[0-9]*" | sort -V | tail -n1)

if [ -z "$LATEST_TAG" ]; then
    # No semantic version tags found, start fresh
    NEW_VERSION="v0.0.1"
    print_warning "No semantic version tags found. Starting with $NEW_VERSION"
else
    # Extract version numbers
    if [[ $LATEST_TAG =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        MAJOR=${BASH_REMATCH[1]}
        MINOR=${BASH_REMATCH[2]}
        PATCH=${BASH_REMATCH[3]}
        
        print_info "Current version: $LATEST_TAG (Major: $MAJOR, Minor: $MINOR, Patch: $PATCH)"
        
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
        
        NEW_VERSION="v${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}"
    else
        print_error "Latest tag '$LATEST_TAG' doesn't follow semantic versioning. Starting fresh."
        NEW_VERSION="v0.0.1"
    fi
fi

print_info "New version will be: $NEW_VERSION"

# Confirm with user
echo
read -p "🤔 Do you want to create release $NEW_VERSION? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "Version bump cancelled."
    exit 0
fi

# Create and push the tag
print_info "Creating tag $NEW_VERSION..."
git tag -a "$NEW_VERSION" -m "Release $NEW_VERSION"

print_info "Pushing tag to origin..."
git push origin "$NEW_VERSION"

print_success "Version bumped to $NEW_VERSION!"
print_info "GitHub Actions will now create a release with this version."
print_info "Monitor the build at: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^.]*\).*/\1/')/actions"

echo
print_info "🎉 Release $NEW_VERSION is on its way!"
