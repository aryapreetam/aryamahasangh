# Secrets Setup Guide

This guide explains how to set up secrets for all platforms (Desktop, Android, Web, iOS) in your Kotlin Multiplatform project.

## Quick Setup

1. **Copy the template file:**
   ```bash
   cp secrets.properties.template secrets.properties
   ```

2. **Fill in your actual values** in `secrets.properties`

3. **Run any target** - secrets are automatically configured!
   ```bash
   # Android
   ./gradlew assembleDebug
   
   # Desktop  
   ./gradlew run
   
   # Web
   ./gradlew wasmJsBrowserDevelopmentRun
   
   # iOS
   ./gradlew iosSimulatorArm64Test
   ```

That's it! The setup script runs automatically before any build/run task.

## Automated Setup

### Gradle Integration
The setup script now runs **automatically** before any build/run task:

- ✅ **Android builds**: `assembleDebug`, `assembleRelease`, etc.
- ✅ **Desktop runs**: `run`, `runDebug`, `runRelease`
- ✅ **Web builds**: `wasmJsBrowserDevelopmentRun`, etc.
- ✅ **iOS builds**: `iosSimulatorArm64Test`, etc.

### Manual Setup (Optional)
You can still run the setup script manually:
```bash
./setup-secrets.sh
```

## What the Setup Script Does

The `setup-secrets.sh` script automatically:

1. **Desktop Platform**: Uses `secrets.properties` from the project root
2. **Android Platform**: Copies `secrets.properties` to `composeApp/src/androidMain/assets/`
3. **Web Platform**: Updates `config.json` with values from `secrets.properties`
4. **iOS Platform**: Copies `secrets.properties` to `iosApp/iosApp/` bundle

## Platform-Specific Details

### Desktop
- **File Location**: `secrets.properties` (project root)
- **Loader**: `DesktopSecretsLoader` tries multiple paths:
  - `secrets.properties` (current directory)
  - `../secrets.properties` (parent directory)
  - `../../secrets.properties` (grandparent directory)

### Android
- **File Location**: `composeApp/src/androidMain/assets/secrets.properties`
- **Loader**: `AndroidSecretsLoader` loads from app assets
- **Note**: The assets directory is created automatically by the setup script

### Web
- **File Location**: `composeApp/src/wasmJsMain/resources/config.json`
- **Loader**: `WebSecretsLoader` fetches the JSON file
- **Note**: The setup script converts properties format to JSON format

### iOS
- **File Location**: `iosApp/iosApp/secrets.properties`
- **Loader**: `IOSSecretsLoader` loads from app bundle resources
- **Note**: The setup script copies the file to the iOS app bundle

## Manual Setup (Alternative)

If you prefer to set up manually:

### 1. Desktop
```bash
# Just ensure secrets.properties exists in project root
cp secrets.properties.template secrets.properties
# Edit with your values
```

### 2. Android
```bash
# Create assets directory
mkdir -p composeApp/src/androidMain/assets
# Copy secrets file
cp secrets.properties composeApp/src/androidMain/assets/
```

### 3. Web
```bash
# Edit config.json with your values
nano composeApp/src/wasmJsMain/resources/config.json
```

### 4. iOS
```bash
# Create iOS app bundle directory (if needed)
mkdir -p iosApp/iosApp
# Copy secrets file
cp secrets.properties iosApp/iosApp/
```

## Configuration Format

### secrets.properties format:
```properties
# Environment
environment=dev

# Development Configuration
dev.supabase.url=https://your-dev.supabase.co
dev.supabase.key=your-dev-key
dev.server.url=http://localhost:4000

# Production Configuration
prod.supabase.url=https://your-prod.supabase.co
prod.supabase.key=your-prod-key
prod.server.url=https://your-prod-server.com
```

### config.json format (Web):
```json
{
  "environment": "dev",
  "dev.supabase.url": "https://your-dev.supabase.co",
  "dev.supabase.key": "your-dev-key",
  "dev.server.url": "http://localhost:4000",
  "prod.supabase.url": "https://your-prod.supabase.co",
  "prod.supabase.key": "your-prod-key",
  "prod.server.url": "https://your-prod-server.com"
}
```

## Security Notes

- ✅ `secrets.properties` is already in `.gitignore`
- ✅ Android assets secrets file is also in `.gitignore`
- ✅ iOS app bundle secrets file is also in `.gitignore`
- ✅ Never commit actual secret values to version control
- ✅ Use the template file for sharing configuration structure

## Troubleshooting

### Desktop: "secrets.properties file not found"
- Ensure the file exists in the project root
- Check the console output for attempted file paths
- Verify file permissions

### Android: "Failed to load secrets from assets"
- Ensure `composeApp/src/androidMain/assets/secrets.properties` exists
- Run the setup script to create it automatically
- Check that the assets directory is included in the build

### Web: "Failed to fetch config.json"
- Ensure `composeApp/src/wasmJsMain/resources/config.json` exists
- Verify the JSON format is valid
- Check browser network tab for fetch errors

### iOS: "secrets.properties not found in iOS bundle"
- Ensure `iosApp/iosApp/secrets.properties` exists
- Run the setup script to create it automatically
- Check that the file is included in the iOS app bundle

### All Platforms: "Configuration not initialized"
- Ensure `ConfigInitializer.initializeBlocking()` is called in main entry points
- Check console logs for initialization errors
- Verify the platform-specific SecretsLoader is working

## Testing

To verify secrets are loading correctly, check the console output when running your app:

```
🔧 Initializing configuration...
✅ Loaded secrets from secrets.properties file: /path/to/secrets.properties
✅ Configuration initialized successfully
📋 Current configuration:
   Environment: dev
   Supabase URL: https://your-dev.supabase.co
   Server URL: http://localhost:4000
```

## Files Modified

This setup includes the following changes:

1. **Fixed Desktop path resolution** in `DesktopSecretsLoader.kt`
2. **Made initialization synchronous** in `ConfigInitializer.kt`
3. **Updated main entry points** to use `initializeBlocking()`
4. **Created setup script** `setup-secrets.sh` with iOS support
5. **Updated .gitignore** to exclude all platform secrets files
6. **Added Gradle automation** in `composeApp/build.gradle.kts`
7. **Created platform-specific secrets files** for all platforms

## Gradle Automation

The build script now includes:

- **`setupSecrets` task**: Automatically runs the setup script
- **`checkSecrets` task**: Warns if secrets.properties is missing
- **Automatic execution**: Setup runs before any build/run task
- **Cross-platform support**: Works on Windows, macOS, and Linux

### Available Gradle Tasks

```bash
# Check if secrets are configured
./gradlew checkSecrets

# Manually run secrets setup
./gradlew setupSecrets

# Any build task automatically runs setup first
./gradlew assembleDebug        # Android
./gradlew run                  # Desktop
./gradlew wasmJsBrowserRun     # Web
./gradlew iosSimulatorArm64Test # iOS
```

The secrets loading now works consistently across all platforms with full automation!