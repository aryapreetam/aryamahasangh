#!/bin/sh

set -e

echo "================================"
echo "Pre-Xcode Build Script"
echo "================================"

# Ensure Java is in PATH for the Xcode build
echo "Setting up Java environment for Xcode build..."
export PATH="/usr/local/opt/openjdk@17/bin:$PATH"
export JAVA_HOME="$(/usr/local/opt/openjdk@17/bin/java -XshowSettings:properties -version 2>&1 | grep 'java.home' | awk '{print $3}')"

echo "Java version:"
java -version

echo "JAVA_HOME: $JAVA_HOME"
echo "================================"
