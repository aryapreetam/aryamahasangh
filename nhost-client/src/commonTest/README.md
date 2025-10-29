# NHost Client Tests

This directory contains tests for the NHost Kotlin Multiplatform client.

## Test Structure

### Integration Tests

**NHostClientIntegrationTest.kt**
- Tests basic client creation and configuration
- Tests Apollo client integration
- Tests storage public URL generation
- Tests authentication state initialization
- Does NOT require Apollo-generated query classes
- ✅ Can run immediately without schema download

### GraphQL Query Tests

**NHostClientTest.kt**
- Tests actual GraphQL queries against the live NHost instance
- Tests the `OrganisationNames` query
- Tests Apollo interceptor functionality
- ⚠️ Requires Apollo-generated query classes (see setup below)

## Running the Tests

### Quick Test (Integration Tests Only)

Run the integration tests that don't require Apollo code generation:

```bash
./gradlew :nhost-client:allTests --tests "com.aryamahasangh.nhost.NHostClientIntegrationTest"
```

### Full Test Suite (Including GraphQL Tests)

1. **First, download the GraphQL schema:**
   ```bash
   ./gradlew :nhost-client:downloadNhostApolloSchemaFromIntrospection
   ```

2. **Generate Apollo sources:**
   ```bash
   ./gradlew :nhost-client:generateNhostApolloSources
   ```

3. **Run all tests:**
   ```bash
   ./gradlew :nhost-client:allTests
   ```

### Platform-Specific Tests

```bash
# Android tests
./gradlew :nhost-client:testDebugUnitTest

# iOS tests
./gradlew :nhost-client:iosX64Test

# Desktop/JVM tests
./gradlew :nhost-client:desktopTest

# WasmJS tests
./gradlew :nhost-client:wasmJsTest
```

## Test Configuration

### NHost Instance

The tests use the following NHost instance:
- **Base URL**: `https://hwdbpplmrdjdhcsmdleh.hasura.ap-south-1.nhost.run`
- **GraphQL URL**: `https://hwdbpplmrdjdhcsmdleh.hasura.ap-south-1.nhost.run/v1/graphql`

### Configuring Credentials (Optional)

For introspection with admin access, add to `local.properties`:

```properties
# Dev environment (default)
nhost_dev_graphql_url=https://hwdbpplmrdjdhcsmdleh.hasura.ap-south-1.nhost.run/v1/graphql
nhost_dev_admin_secret=your-admin-secret-here
```

If no credentials are provided, the tests will use public access (which is sufficient for most tests).

## Test Coverage

### ✅ Covered
- Client creation and configuration
- Apollo client integration
- Storage public URL generation
- Authentication state management
- Multiple client instances
- Configuration variations

### 📝 Planned (When Credentials Available)
- Sign in with email/password
- Sign out
- Token refresh
- File upload
- File deletion
- Presigned URL generation

## GraphQL Query Being Tested

```graphql
query OrganisationNames {
  organisation {
    id
    name
  }
}
```

This query fetches all organisations with their ID and name fields.

## Expected Test Output

When running `NHostClientIntegrationTest`:

```
✓ Public URL generated: https://hwdbpplmrdjdhcsmdleh.hasura.ap-south-1.nhost.run/v1/storage/files/test-file-id-12345
✓ Auth state initialized correctly
✓ Multiple independent client instances created successfully
✓ Client configuration variations work correctly
```

When running `NHostClientTest` (requires Apollo sources):

```
✓ Successfully fetched 5 organisations
  - Organisation: id=1, name=Arya Mahasangh
  - Organisation: id=2, name=Sample Org
  ...
✓ Apollo interceptor is working correctly
```

## Troubleshooting

### Issue: Apollo sources not generated

**Solution**: Run schema download and generation:
```bash
./gradlew :nhost-client:downloadNhostApolloSchemaFromIntrospection
./gradlew :nhost-client:generateNhostApolloSources
```

### Issue: Introspection fails

**Possible causes**:
1. Network connectivity issues
2. NHost instance is down
3. Admin secret is required but not provided

**Solution**: Check the NHost URL and add admin secret to `local.properties` if required.

### Issue: Tests fail with "Unresolved reference: OrganisationNamesQuery"

**Solution**: The Apollo sources haven't been generated yet. Follow the "Full Test Suite" instructions above.

## Adding New Tests

1. Create a new test file in `src/commonTest/kotlin/com/aryamahasangh/nhost/`
2. Use `@Test` annotation for each test
3. Use `runTest` for suspending test functions
4. Run with `./gradlew :nhost-client:allTests`

Example:

```kotlin
@Test
fun testMyNewFeature() = runTest {
    val client = createNHostClient(nhostUrl = NHOST_URL)
    
    // Your test code here
    
    client.close()
}
```

## Continuous Integration

These tests can be run in CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run NHost Client Tests
  run: |
    ./gradlew :nhost-client:downloadNhostApolloSchemaFromIntrospection
    ./gradlew :nhost-client:allTests
```

## Notes

- Tests use `runTest` from `kotlinx-coroutines-test` for coroutine testing
- Integration tests are safe to run without credentials
- GraphQL tests require schema generation
- All tests clean up resources in `@AfterTest`

