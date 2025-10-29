# Configuration Cache Fix for fixGeneratedSecrets Task

## Problem

When enabling `org.gradle.configuration-cache=true` in `gradle.properties`, the build failed with the following error:

```
Configuration cache problems found in this build.
3 problems were found storing the configuration cache.
- Task `:nhost-client:fixGeneratedSecrets` of type `org.gradle.api.DefaultTask`: 
  cannot serialize object of type 'org.gradle.api.internal.project.DefaultProject', 
  a subtype of 'org.gradle.api.Project', as these are not supported with the configuration cache.
- Task `:nhost-client:fixGeneratedSecrets` of type `org.gradle.api.DefaultTask`: 
  invocation of 'Task.project' at execution time is unsupported with the configuration cache.
```

## Root Cause

The `fixGeneratedSecrets` task was accessing the `project` object and `layout` API at **execution time** (inside the `doLast` block), which violates configuration cache requirements. The configuration cache cannot serialize `Project` instances.

### Original Code (Problematic)
```kotlin
tasks.register("fixGeneratedSecrets") {
  doLast {
    // ❌ Accessing layout at execution time
    val secretsFile = layout.buildDirectory
      .dir("generated/kmp-secrets/commonMain/kotlin/secrets/Secrets.kt")
      .get()
      .asFile

    if (secretsFile.exists()) {
      // ❌ Accessing project.name at execution time
      println("[${project.name}] Fixing dollar signs...")
      // ...
    }
  }
}
```

## Solution

The fix captures all required values at **configuration time** (outside the `doLast` block) and uses Gradle's Provider API for lazy evaluation:

### Fixed Code
```kotlin
tasks.register("fixGeneratedSecrets") {
  // ✅ Capture values at configuration time
  val projectName = project.name
  val secretsFileProvider = layout.buildDirectory
    .dir("generated/kmp-secrets/commonMain/kotlin/secrets/Secrets.kt")
    .map { it.asFile }

  doLast {
    // ✅ Resolve Provider at execution time
    val secretsFile = secretsFileProvider.get()

    if (secretsFile.exists()) {
      // ✅ Use captured projectName
      println("[$projectName] Fixing dollar signs...")
      // ...
    }
  }
}
```

## Key Changes

1. **Captured `project.name` at configuration time**: Stored the project name in a local variable before the `doLast` block
2. **Used Provider API for file paths**: Changed from `.get().asFile` to `.map { it.asFile }` to create a Provider that's resolved at execution time
3. **Resolved Provider in `doLast`**: Called `.get()` on the Provider inside the execution block

## Benefits

- ✅ **Configuration Cache Compatible**: No more serialization errors
- ✅ **Faster Builds**: Configuration cache can be reused across builds
- ✅ **Same Functionality**: The task behavior remains unchanged
- ✅ **Multi-Module Support**: Works correctly for both `nhost-client` and `composeApp` modules

## Verification

The fix was tested with:
```bash
./gradlew :nhost-client:generateSecretsMetadataCommonMain --configuration-cache
./gradlew :composeApp:generateSecretsMetadataCommonMain --configuration-cache
```

Both builds succeeded with:
- ✅ "Configuration cache entry stored" on first run
- ✅ "Configuration cache entry reused" on subsequent runs
- ✅ No configuration cache warnings or errors

## References

- [Gradle Configuration Cache Requirements](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html)
- [Gradle Provider API](https://docs.gradle.org/current/userguide/lazy_configuration.html)
- [Configuration Cache: Disallowed Types](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html#config_cache:requirements:disallowed_types)

