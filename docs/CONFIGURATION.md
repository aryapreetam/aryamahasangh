# Configuration Management

This project uses a simplified, secure configuration system that eliminates the need for platform-specific configuration files.

## Overview

The configuration system follows these principles:
- **Single source of truth**: One `AppConfig.kt` file for all platforms
- **Environment separation**: Dev and prod configurations
- **Security first**: Secrets never committed to version control
- **Flexible loading**: Properties file for development, environment variables for production

## Configuration Files

### `secrets.properties` (Development)
- Contains actual secrets and configuration values
- **Never committed** to version control (gitignored)
- Used during local development
- Supports dev/prod environment separation

### `secrets.properties.template` (Template)
- Template file showing required configuration keys
- **Safe to commit** (contains no actual secrets)
- Used by developers to create their local `secrets.properties`

### `AppConfig.kt` (Code)
- Single configuration object for all platforms
- Loads from properties file or environment variables
- No platform-specific implementations needed
- No hard-coded secrets

## Setup for Development

1. **Initial Setup**:
   ```bash
   ./setup-dev.sh
   ```
   This creates `secrets.properties` from the template.

2. **Configure Secrets**:
   Edit `secrets.properties` with your actual values:
   ```properties
   # Development Configuration
   dev.supabase.url=https://your-dev-project.supabase.co
   dev.supabase.key=your-dev-anon-key
   dev.server.url=http://localhost:4000

   # Production Configuration  
   prod.supabase.url=https://your-prod-project.supabase.co
   prod.supabase.key=your-prod-anon-key
   prod.server.url=https://your-production-server.com

   # Current environment (dev or prod)
   environment=dev
   ```

3. **Start Development**:
   ```bash
   ./gradlew build
   ```

## Configuration Loading Priority

The system loads configuration in this order:

1. **Environment Variables** (Production/CI):
   - `SUPABASE_URL`
   - `SUPABASE_KEY`
   - `SERVER_URL`
   - `ENVIRONMENT=prod`

2. **secrets.properties File** (Development):
   - `dev.supabase.url` or `prod.supabase.url`
   - `dev.supabase.key` or `prod.supabase.key`
   - `dev.server.url` or `prod.server.url`
   - `environment=dev` or `environment=prod`

3. **Error** if not found in either location

## Environment Switching

### Development (Local)
```properties
# In secrets.properties
environment=dev
```
Uses `dev.*` prefixed values from `secrets.properties`.

### Production (CI/CD)
```bash
# Environment variables
export ENVIRONMENT=prod
export SUPABASE_URL=https://your-prod-project.supabase.co
export SUPABASE_KEY=your-prod-anon-key
export SERVER_URL=https://your-production-server.com
```

## Usage in Code

```kotlin
import org.aryamahasangh.config.AppConfig

// Configuration is loaded automatically
val supabaseUrl = AppConfig.supabaseUrl
val graphqlUrl = AppConfig.graphqlUrl

// Debug info (safe - doesn't expose secrets)
println(AppConfig.getConfigInfo())
```

## Security Features

### ✅ What's Secure
- Secrets never in source code
- `secrets.properties` is gitignored
- Environment variables in production
- Template file shows structure without secrets

### ❌ What to Avoid
- Never commit `secrets.properties`
- Never hard-code secrets in code
- Never put secrets in build files
- Never share secrets in chat/email

## CI/CD Integration

### GitHub Actions
```yaml
env:
  ENVIRONMENT: prod
  SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
  SUPABASE_KEY: ${{ secrets.SUPABASE_KEY }}
  SERVER_URL: ${{ secrets.SERVER_URL }}
```

### Docker
```dockerfile
ENV ENVIRONMENT=prod
ENV SUPABASE_URL=${SUPABASE_URL}
ENV SUPABASE_KEY=${SUPABASE_KEY}
ENV SERVER_URL=${SERVER_URL}
```

## Migration from Old System

If you're migrating from platform-specific config files:

1. **Remove old files**:
   - `composeApp/src/androidMain/kotlin/.../config/AppConfig.kt`
   - `composeApp/src/iosMain/kotlin/.../config/AppConfig.kt`
   - `composeApp/src/desktopMain/kotlin/.../config/AppConfig.kt`
   - `composeApp/src/wasmJsMain/kotlin/.../config/AppConfig.kt`

2. **Update imports**:
   ```kotlin
   // Old (platform-specific)
   import org.aryamahasangh.config.getAppConfig

   // New (unified)
   import org.aryamahasangh.config.AppConfig
   ```

3. **Update usage**:
   ```kotlin
   // Old
   val config = getAppConfig()
   val url = config.supabaseUrl

   // New
   val url = AppConfig.supabaseUrl
   ```

## Troubleshooting

### "Configuration not found" Error
1. Check if `secrets.properties` exists
2. Verify the file has the correct keys
3. Check environment variable names in production

### Wrong Environment
1. Check `environment=dev` or `environment=prod` in `secrets.properties`
2. Check `ENVIRONMENT=prod` environment variable in production

### Build Issues
1. Run `./setup-dev.sh` to create missing files
2. Verify `secrets.properties` has all required keys
3. Check that values don't contain special characters that need escaping

## Benefits of This Approach

1. **Simplified**: No platform-specific configuration files
2. **Secure**: Secrets never in version control
3. **Flexible**: Easy to switch between dev/prod
4. **Maintainable**: Single configuration object
5. **Standard**: Similar to Android's `local.properties`
6. **CI/CD Ready**: Works with environment variables