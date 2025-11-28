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
echo "Post-clone script completed"
echo "Gradle tasks will run during Xcode build phase"
echo "================================"
