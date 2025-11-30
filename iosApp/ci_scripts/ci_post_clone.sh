#!/bin/sh

set -e

echo "================================"
echo "Xcode Cloud Post-Clone Script"
echo "================================"

# Navigate to the repository root
cd "$CI_PRIMARY_REPOSITORY_PATH"

echo "Current directory: $(pwd)"

# Install Java (required for Gradle/KMP builds)
echo "Installing Java via Homebrew..."
brew install openjdk@17

# Add Java to PATH for this session
echo "Setting up Java environment..."
export PATH="/usr/local/opt/openjdk@17/bin:$PATH"
export JAVA_HOME="$(/usr/local/opt/openjdk@17/bin/java -XshowSettings:properties -version 2>&1 | grep 'java.home' | awk '{print $3}')"

# Verify Java installation
echo "Java version:"
java -version

# Make gradlew executable
echo "Making gradlew executable..."
chmod +x ./gradlew

# Setup local.properties if it doesn't exist
if [ ! -f "local.properties" ]; then
    echo "Creating local.properties from Xcode Cloud environment variables..."
    
    # Create local.properties with values from environment variables
    cat > local.properties << EOF
# Auto-generated for Xcode Cloud CI
app_version=${app_version:-1.0.17}
dev_supabase_key=${dev_supabase_key:-}
dev_supabase_url=${dev_supabase_url:-}
staging_supabase_key=${staging_supabase_key:-}
staging_supabase_url=${staging_supabase_url:-}
prod_supabase_key=${prod_supabase_key:-}
prod_supabase_url=${prod_supabase_url:-}
sentry=${sentry:-}
environment=${environment:-dev}
EOF

    echo "✅ local.properties created with environment variables"
    echo "   app_version: ${app_version:-1.0.6}"
    echo "   environment: ${environment:-dev}"
    echo "   sentry: ${sentry:-<not set>}"
fi

echo "================================"
echo "Downloading Sentry Cocoa framework"
echo "================================"

SENTRY_VERSION="8.57.3"
SENTRY_URL="https://github.com/getsentry/sentry-cocoa/releases/download/$SENTRY_VERSION/Sentry.xcframework.zip"

echo "Downloading Sentry.xcframework (v$SENTRY_VERSION)..."
curl -L "$SENTRY_URL" -o sentry.zip

echo "Unzipping..."
unzip -o sentry.zip -d sentry

echo "Sentry.xcframework available at: $(pwd)/sentry/Sentry.xcframework"

echo "================================"
echo "Post-clone script completed"
echo "Gradle tasks will run during Xcode build phase"
echo "================================"

export GRADLE_OPTS="-Xmx6144m -XX:MaxMetaspaceSize=1024m"
export _JAVA_OPTIONS="-Xmx6144m -XX:MaxMetaspaceSize=1024m"
export JAVA_TOOL_OPTIONS="-Xmx6144m -XX:MaxMetaspaceSize=1024m"
export KOTLIN_OPTS="-Xmx6144m"

echo "org.gradle.workers.max=1" >> gradle.properties
echo "org.gradle.worker.max-memory=6144m" >> gradle.properties
echo "org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1024m" >> gradle.properties

echo "==============================="
echo "Downloading ComposeApp XCFramework from GitHub Actions"
echo "==============================="

# -----------------------------
# REQUIRE GITHUB TOKEN
# -----------------------------
if [ -z "$GITHUB_TOKEN_CI" ]; then
  echo "⚠️  WARNING: Missing GITHUB_TOKEN_CI."
  echo "Add a GitHub PAT with 'repo' scope in Xcode Cloud > Environment Variables."
  echo "Skipping XCFramework download - build may fail if framework is needed."
else
  # -----------------------------
  # FETCH LATEST SUCCESSFUL WORKFLOW RUN
  # -----------------------------
  REPO="aryapreetam/aryamahasangh"
  WORKFLOW_FILE="build-ios-xcframework.yml"
  ARTIFACT_NAME="ComposeApp.xcframework"

  RUNS_API="https://api.github.com/repos/${REPO}/actions/workflows/${WORKFLOW_FILE}/runs"

  echo "Fetching latest successful workflow run..."
  RUN_ID=$(curl -s -H "Authorization: token ${GITHUB_TOKEN_CI}" "${RUNS_API}" \
    | jq -r '.workflow_runs[] | select(.conclusion=="success") | .id' \
    | head -n 1)

  if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
    echo "⚠️  WARNING: No successful workflow runs found for ${WORKFLOW_FILE}."
    echo "Please run the 'Build iOS XCFramework' GitHub Action first."
    echo "Continuing without XCFramework - build may fail."
  else
    echo "Latest successful run: $RUN_ID"

    # -----------------------------
    # LOCATE ARTIFACT DOWNLOAD URL
    # -----------------------------
    ARTIFACTS_API="https://api.github.com/repos/${REPO}/actions/runs/${RUN_ID}/artifacts"

    echo "Fetching artifact metadata..."
    ART_URL=$(curl -s -H "Authorization: token ${GITHUB_TOKEN_CI}" "${ARTIFACTS_API}" \
      | jq -r --arg name "${ARTIFACT_NAME}" '.artifacts[] | select(.name == $name) | .archive_download_url')

    if [ -z "$ART_URL" ] || [ "$ART_URL" = "null" ]; then
      echo "⚠️  WARNING: Artifact '${ARTIFACT_NAME}' not found for run ${RUN_ID}."
      echo "Available artifacts:"
      curl -s -H "Authorization: token ${GITHUB_TOKEN_CI}" "${ARTIFACTS_API}" | jq '.artifacts[].name'
      echo "Continuing without XCFramework - build may fail."
    else
      echo "Artifact found: $ART_URL"

      # -----------------------------
      # DOWNLOAD ZIP ARTIFACT
      # -----------------------------
      ZIP_FILE="${ARTIFACT_NAME}.zip"

      echo "Downloading XCFramework artifact..."
      curl -L \
        -H "Authorization: token ${GITHUB_TOKEN_CI}" \
        -H "Accept: application/vnd.github+json" \
        "$ART_URL" \
        -o "$ZIP_FILE"

      echo "Download complete: $ZIP_FILE"

      # -----------------------------
      # UNZIP & PLACE INTO PROJECT
      # -----------------------------
      echo "Unzipping XCFramework..."
      rm -rf xcframeworks
      mkdir xcframeworks
      unzip -o "$ZIP_FILE" -d xcframeworks

      # Move XCFramework to sentry folder (alongside Sentry.xcframework)
      echo "Placing XCFramework in sentry/ folder..."
      rm -rf sentry/ComposeApp.xcframework
      mv xcframeworks/ComposeApp.xcframework sentry/

      echo "✅ XCFramework installed at:"
      echo "   $(pwd)/sentry/ComposeApp.xcframework"
      
      # Verify framework structure
      if [ -d "sentry/ComposeApp.xcframework" ]; then
        echo "✅ XCFramework structure verified:"
        ls -la sentry/ComposeApp.xcframework/
      else
        echo "❌ ERROR: XCFramework not found after extraction!"
      fi
    fi
  fi
fi

echo "==============================="
echo "XCFramework setup complete."
echo "Xcode Cloud can now build the iOS app."
echo "==============================="
