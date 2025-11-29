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
app_version=${app_version:-1.0.6}
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
