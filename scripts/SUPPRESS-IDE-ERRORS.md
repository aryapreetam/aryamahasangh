# 🚫 Suppress IDE Errors for Kotlin Scripts

## ⚡ Quick Fix - Choose ONE Method

### **Method 1: File Type Override (Simplest)**

Already configured in `.idea/fileTypes.xml` - treats .kts files as plain text.

- ✅ **Pros:** Instant fix, no red squiggly lines
- ❌ **Cons:** No syntax highlighting

### **Method 2: Inspection Suppression (Best)**

Already configured in `.idea/inspections.xml` - keeps syntax highlighting but removes errors.

- ✅ **Pros:** Syntax highlighting + no errors
- ❌ **Cons:** Requires IDE restart

### **Method 3: Manual IDE Settings**

**For IntelliJ IDEA:**

1. `Settings` → `Editor` → `Inspections`
2. Search for "Unresolved reference"
3. Click on `Kotlin` → `Unresolved reference`
4. Click "Add" next to file mask
5. Add pattern: `scripts/*.kts`
6. Set severity to "No highlighting"

**For VS Code:**
Add to `.vscode/settings.json`:

```json
{
  "files.associations": {
    "scripts/*.kts": "plaintext"
  }
}
```

### **Method 4: Per-File Suppression**

Add to the top of each .kts file:

```kotlin
@file:Suppress(
    "UnresolvedReference",
    "UnusedImport", 
    "RedundantSuspendModifier",
    "UNUSED_VARIABLE"
)
```

## 🔧 Alternative: Gradle-Based Utilities

If you prefer ZERO IDE issues, use the Gradle-based utilities instead:

```bash
# Instead of: kotlin scripts/pre-flight-check.main.kts
./gradlew :migration-utilities:preFlightCheck

# Instead of: kotlin scripts/environment-diff.main.kts  
./gradlew :migration-utilities:environmentDiff

# Instead of: kotlin scripts/auto-configure-graphql.main.kts
./gradlew :migration-utilities:autoConfigureGraphql
```

## 📋 Verification Steps

After applying any method:

1. **Restart IDE** (important!)
2. **Open any .kts file**
3. **Verify no red squiggly lines**
4. **Test script execution:**
   ```bash
   kotlin scripts/pre-flight-check.main.kts
   ```

## 🎯 Recommended Approach

**For Development:** Use Method 2 (Inspection Suppression)

- Keeps syntax highlighting
- Eliminates all error indicators
- Scripts remain fully functional

**For Production:** Consider Gradle-based utilities

- Professional build setup
- Proper IDE integration
- Better dependency management

## 🐛 Troubleshooting

**IDE still shows errors?**

1. Clear IDE caches: `File` → `Invalidate Caches and Restart`
2. Reimport project: `File` → `Reload Gradle Project`
3. Check if `.idea/` folder is properly configured

**Scripts don't execute?**

1. Verify Kotlin installation: `kotlin -version`
2. Check file permissions: `chmod +x scripts/*.kts`
3. Test with simple script first

**Build issues?**

1. Check Gradle version compatibility
2. Verify internet connection for dependency download
3. Clear Gradle cache: `./gradlew clean`

---

**The goal is achieved: NO red squiggly lines while maintaining full script functionality!**
