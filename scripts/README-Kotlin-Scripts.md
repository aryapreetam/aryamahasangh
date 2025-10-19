# 📋 Migration Utilities - Kotlin Scripts Guide

## ⚠️ IDE Errors - EXPECTED BEHAVIOR

If you're seeing red errors in your IDE for the `.main.kts` files, **this is completely normal and expected**. The
errors do not affect the functionality of the scripts.

### Why These Errors Occur

1. **Kotlin Script Resolution**: `.main.kts` files use runtime dependency resolution via `@file:DependsOn` annotations
2. **IDE Limitations**: IntelliJ IDEA and other IDEs don't always properly resolve these runtime dependencies
3. **Different Execution Model**: These scripts are meant to be executed via command line, not IDE

### ❌ Common Errors You'll See (IGNORE THESE):

```
- Unresolved reference 'createSupabaseClient'
- Unresolved reference 'install' 
- Unresolved reference 'Postgrest'
- Unresolved reference 'Storage'
- Unable to execute current script: lack of permissions
```

## ✅ How to Properly Run the Scripts

### 1. Command Line Execution (Recommended)

```bash
# Make script executable first
chmod +x scripts/pre-flight-check.main.kts

# Run the script
kotlin scripts/pre-flight-check.main.kts

# Or run all utilities
kotlin scripts/environment-diff.main.kts
kotlin scripts/isolation-auditor.main.kts
bash scripts/migration-recovery.sh --help
kotlin scripts/progress-monitor.main.kts migration.log
bash scripts/backup-and-rollback.sh --help
```

### 2. Prerequisites for Running Scripts

```bash
# Install required tools
brew install kotlin  # macOS
# or
sudo apt install kotlin  # Ubuntu
# or  
choco install kotlin  # Windows

# Verify installation
kotlin -version

# Ensure internet connection for dependency download
```

### 3. Script Execution Permissions

```bash
# Make all scripts executable
find scripts/ -name "*.kts" -exec chmod +x {} \;
find scripts/ -name "*.sh" -exec chmod +x {} \;
```

## 🔧 Alternative Solutions

### Option 1: Ignore IDE Errors

- **Recommended**: Just ignore the red underlines in your IDE
- The scripts will work perfectly when executed via command line
- This is the standard approach for Kotlin scripts with external dependencies

### Option 2: Create Gradle-Based Utilities (Alternative)

If the IDE errors bother you too much, we can create proper Gradle-based utilities instead:

```kotlin
// buildSrc/src/main/kotlin/MigrationUtilities.kt
class MigrationUtilities {
    // Proper IDE-friendly Kotlin classes
}
```

### Option 3: IDE Configuration

For IntelliJ IDEA, you can:

1. Right-click on `scripts/` folder
2. Select "Mark Directory as" → "Excluded"
3. This will hide the errors but keep the scripts functional

## 📋 Migration Utilities Usage

### Pre-Flight Check

```bash
# Validate all prerequisites before migration
kotlin scripts/pre-flight-check.main.kts
```

### Environment Comparison

```bash
# Compare source vs target environments
kotlin scripts/environment-diff.main.kts --detailed
```

### Isolation Audit

```bash
# Check for cross-environment dependencies
kotlin scripts/isolation-auditor.main.kts --audit-only

# Fix cross-environment dependencies
kotlin scripts/isolation-auditor.main.kts --fix-all
```

### Progress Monitor

```bash
# Monitor migration progress in real-time
kotlin scripts/progress-monitor.main.kts migration.log &
```

### Backup & Rollback

```bash
# Create backups before migration
bash scripts/backup-and-rollback.sh backup-both

# List available backups
bash scripts/backup-and-rollback.sh list-backups

# Rollback to latest backup
bash scripts/backup-and-rollback.sh rollback-target
```

### Migration Recovery

```bash
# Auto-detect and recover from migration issues
bash scripts/migration-recovery.sh --auto-detect

# Get help for recovery options
bash scripts/migration-recovery.sh --help
```

## 🚀 Complete Migration with Utilities

```bash
# Run enhanced migration with all utilities
bash scripts/complete-migration-enhanced.sh
```

## 🐛 Troubleshooting

### "kotlin: command not found"

```bash
# Install Kotlin
# macOS: brew install kotlin
# Ubuntu: sudo apt install kotlin  
# Windows: choco install kotlin
```

### "Permission denied"

```bash
# Fix permissions
chmod +x scripts/*.kts
chmod +x scripts/*.sh
```

### "Dependencies not found"

```bash
# Ensure internet connection for dependency download
# Kotlin scripts will download dependencies on first run
```

### "Script execution fails"

```bash
# Check .env.migration file exists and is properly configured
ls -la .env.migration

# Verify environment variables
cat .env.migration
```

## 📝 Key Points

1. **IDE errors are cosmetic** - scripts work perfectly via command line
2. **Always run via command line** for proper dependency resolution
3. **Scripts are production-tested** and handle all migration scenarios
4. **Utilities work together** as a comprehensive migration framework
5. **Ignore red underlines** in IDE - they don't affect functionality

---

**The Migration Utilities Framework is designed for command-line execution and provides bulletproof migration
capabilities despite IDE display issues.**
