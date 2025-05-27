# Development Guide

## Prerequisites

- **JDK 17** or higher
- **Android Studio** (for Android development)
- **IntelliJ IDEA** (recommended for multiplatform development)
- **Git** for version control

## Setup

### 1. Clone the Repository
```bash
git clone https://github.com/aryapreetam/aryamahasangh.git
cd aryamahasangh
```

### 2. Configure Environment
```bash
# Copy the template and fill in your values
cp secrets.properties.template secrets.properties
```

Edit `secrets.properties` with your actual values:
```properties
dev.supabase.url=your_dev_supabase_url
dev.supabase.key=your_dev_supabase_key
prod.supabase.url=your_prod_supabase_url
prod.supabase.key=your_prod_supabase_key
```

### 3. Build the Project
```bash
./gradlew build
```

## Running the Application

### Android
```bash
./gradlew :composeApp:installDebug
```

### Desktop
```bash
./gradlew :composeApp:run
```

### Web
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

## Development Workflow

### Code Quality Checks

#### Format Code
```bash
./gradlew ktlintFormat
```

#### Check Code Style
```bash
./gradlew ktlintCheck
```

#### Static Analysis
```bash
./gradlew detekt
```

#### Run All Quality Checks
```bash
./gradlew ktlintCheck detekt test
```

### Testing

#### Run All Tests
```bash
./gradlew test
```

#### Run Specific Test
```bash
./gradlew :composeApp:testDebugUnitTest --tests "ActivityRepositoryTest"
```

#### Generate Test Coverage Report
```bash
./gradlew koverHtmlReport
```

### Building

#### Debug Build
```bash
./gradlew assembleDebug
```

#### Release Build
```bash
./gradlew assembleRelease
```

## Project Structure Guidelines

### Adding New Features

1. **Create Use Case** (if needed)
   ```kotlin
   // domain/usecase/NewFeatureUseCase.kt
   class NewFeatureUseCase(private val repository: Repository) {
       suspend operator fun invoke(): Result<Data> {
           // Business logic here
       }
   }
   ```

2. **Update Repository** (if needed)
   ```kotlin
   // repository/NewFeatureRepository.kt
   interface NewFeatureRepository {
       suspend fun getData(): Result<Data>
   }
   
   class NewFeatureRepositoryImpl : NewFeatureRepository {
       override suspend fun getData(): Result<Data> {
           // Implementation
       }
   }
   ```

3. **Create ViewModel**
   ```kotlin
   // viewmodel/NewFeatureViewModel.kt
   class NewFeatureViewModel(
       private val useCase: NewFeatureUseCase
   ) : BaseViewModel<NewFeatureUiState>(NewFeatureUiState()) {
       // ViewModel logic
   }
   ```

4. **Create UI State**
   ```kotlin
   data class NewFeatureUiState(
       val data: List<Item> = emptyList(),
       val isLoading: Boolean = false,
       val error: String? = null
   )
   ```

5. **Create Composable Screen**
   ```kotlin
   // ui/screens/NewFeatureScreen.kt
   @Composable
   fun NewFeatureScreen(viewModel: NewFeatureViewModel) {
       // UI implementation
   }
   ```

6. **Add Tests**
   ```kotlin
   // commonTest/.../NewFeatureViewModelTest.kt
   class NewFeatureViewModelTest {
       @Test
       fun `test feature behavior`() {
           // Test implementation
       }
   }
   ```

### Code Style Guidelines

#### Naming Conventions
- **Classes**: PascalCase (`ActivityRepository`)
- **Functions**: camelCase (`loadActivities`)
- **Variables**: camelCase (`isLoading`)
- **Constants**: UPPER_SNAKE_CASE (`DEFAULT_TTL_MS`)

#### File Organization
- One public class per file
- File name matches the main class name
- Group related functionality in packages

#### Documentation
- Document public APIs with KDoc
- Include parameter descriptions
- Add usage examples for complex functions

### Error Handling

#### Use Standardized Error Types
```kotlin
// Good
return Result.Error("Validation failed", ValidationError.RequiredField("name"))

// Avoid
return Result.Error("Error occurred")
```

#### Handle All Error Cases
```kotlin
when (result) {
    is Result.Success -> handleSuccess(result.data)
    is Result.Error -> handleError(result.message)
    is Result.Loading -> showLoading()
}
```

### Testing Guidelines

#### Unit Test Structure
```kotlin
class FeatureTest {
    @BeforeTest
    fun setup() {
        // Test setup
    }
    
    @Test
    fun `should return success when operation succeeds`() {
        // Given
        val input = createTestInput()
        
        // When
        val result = feature.execute(input)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedData, result.getOrNull())
    }
    
    @AfterTest
    fun tearDown() {
        // Cleanup
    }
}
```

#### Mock External Dependencies
```kotlin
private val repository = mockk<Repository>()
private val useCase = FeatureUseCase(repository)

@Test
fun `test with mocked dependency`() {
    // Given
    coEvery { repository.getData() } returns Result.Success(testData)
    
    // When & Then
    // Test implementation
}
```

### Performance Guidelines

#### Use Efficient Data Structures
- Prefer `List` over `Array` for immutable collections
- Use `StateFlow` for UI state
- Implement proper caching strategies

#### Optimize Compose Performance
- Use `remember` for expensive calculations
- Implement proper key strategies for lists
- Avoid unnecessary recompositions

#### Memory Management
- Dispose of resources properly
- Use appropriate coroutine scopes
- Implement cache size limits

## Debugging

### Enable Debug Logging
```kotlin
// In development builds
if (BuildConfig.DEBUG) {
    println("Debug: $message")
}
```

### Common Issues

#### Build Failures
1. Clean and rebuild: `./gradlew clean build`
2. Check Java version: `java -version`
3. Verify secrets.properties exists and is configured

#### Test Failures
1. Run tests individually to isolate issues
2. Check mock configurations
3. Verify test data setup

#### Runtime Issues
1. Check network connectivity
2. Verify API endpoints
3. Review error logs

## Contributing

### Before Submitting PR

1. **Run Quality Checks**
   ```bash
   ./gradlew ktlintCheck detekt test
   ```

2. **Update Documentation**
   - Update README if needed
   - Add/update code comments
   - Update architecture docs for significant changes

3. **Test Thoroughly**
   - Add tests for new functionality
   - Verify existing tests pass
   - Test on multiple platforms if possible

### PR Guidelines

- Use descriptive commit messages
- Keep PRs focused and small
- Include tests for new features
- Update documentation as needed
- Follow the existing code style

### Code Review Checklist

- [ ] Code follows style guidelines
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] No hardcoded values
- [ ] Error handling is implemented
- [ ] Performance considerations addressed

## Deployment

### Environment Setup

#### Development
- Use development API endpoints
- Enable debug logging
- Use test data when possible

#### Production
- Use production API endpoints
- Disable debug logging
- Implement proper error reporting

### Release Process

1. **Update Version**
   ```kotlin
   // In build.gradle.kts
   version = "1.0.1"
   ```

2. **Create Release Build**
   ```bash
   ./gradlew assembleRelease
   ```

3. **Test Release Build**
   - Verify functionality
   - Check performance
   - Test on target devices

4. **Tag Release**
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

## Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://github.com/JetBrains/compose-multiplatform)
- [Apollo GraphQL Documentation](https://www.apollographql.com/docs/kotlin/)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

## Support

For questions or issues:
1. Check existing documentation
2. Search through GitHub issues
3. Create a new issue with detailed description
4. Contact the development team