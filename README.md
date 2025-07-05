# Arya Mahasangh - Organization Management Platform

A Kotlin Multiplatform project for managing Arya Samaj organizations, members, and activities. Built with Compose
Multiplatform targeting Android, iOS, Web, Desktop, and Server.

## 📚 Documentation

### 📋 Planning & Development

- **[Development Planning](docs/planning/)** - Active development plans and roadmaps
    - [CRUD Integration Plan](docs/planning/2025-06-27-crud-integration-plan.md) - Step-by-step CRUD implementation plan
- **[Development Logs](docs/development-logs/)** - Historical development records
    - [Supabase Setup](docs/development-logs/2025-06-26-initial-supabase-setup-and-functions.md)
    - [CRUD Functions](docs/development-logs/2025-06-27-supabase-crud-functions.md)

### 🏗️ Technical Documentation

- **[Architecture](docs/architecture/)** - System architecture and design decisions
- **[Configuration](docs/CONFIGURATION.md)** - Project configuration guide
- **[Development Guide](docs/DEVELOPMENT.md)** - Development setup and guidelines
- **[Authentication & Security](docs/AUTHENTICATION_SECURITY.md)** - Security implementation details

### 🔧 Setup & Configuration

- **[Secrets Management](docs/SECRETS_SETUP_GUIDE.md)** - How to configure project secrets
- **[Error Handling](docs/ERROR_HANDLING_GUIDE.md)** - Error handling patterns and best practices

### 🗺️ Features & Integration

- **[OpenStreetMap Integration](docs/OpenStreetMapIntegration.md)** - Map functionality implementation
- **[Map Implementation Status](docs/MapImplementationStatus.md)** - Current map feature status

## 🏢 Project Structure

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop, Server.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that's common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple's CoreCrypto for the iOS part of your Kotlin app,
      `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you're sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* `/server` is for the Ktor server application.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here
  too.

## 🚀 Quick Start

### Prerequisites

- Java 11 or higher
- Android SDK (for Android builds)
- Xcode (for iOS builds, macOS only)
- Node.js (for web builds)

### Setup

1. Clone the repository
2. Copy `local.properties.template` to `local.properties` (or run `./setup-dev.sh`)
3. Fill in your configuration values in `local.properties`
4. Run: `./gradlew compileKotlinMetadata` to generate the Secrets object

## 🛠️ Build Commands

### Release Build

```bash
./gradlew :composeApp:assembleRelease
```

### Download GraphQL Schema

```bash
./gradlew downloadApolloSchema
```

## 📦 Release Management

This project uses version management where both local development and CI use the same version number defined in the
template.

### Creating a New Release

Push your changes to the `dev` branch:

```bash
git checkout dev
git add .
git commit -m "Your changes"
git push origin dev
```

The GitHub Actions workflow will automatically:

1. Read version from `local.properties.template` (e.g., 1.0.6)
2. Build Android APK and Web distribution
3. Create a GitHub release with artifacts
4. Deploy web app to Netlify

### Version Management

- **Single Source**: Version defined in `local.properties.template` as `app_version=1.0.6`
- **Consistent**: Same version used locally and in CI builds
- **Manual Updates**: Update `app_version` in template when you want a new release version
- **Fallback**: If no version found in template, CI generates from commit count

### Updating Version for Release

To create a new version:

1. **Update the template:**
   ```bash
   # Edit local.properties.template
   app_version=1.0.7
   ```

2. **Commit and push:**
   ```bash
   git add local.properties.template
   git commit -m "Bump version to 1.0.7"
   git push origin dev
   ```

3. **CI automatically creates release** with version 1.0.7

### Release Artifacts

Each release includes:

- 📱 **AryaMahasangh_v1.0.7.apk** - Android application
- 🌐 **Archive.zip** - Web application files

### Monitoring Builds

Monitor build progress at: [GitHub Actions](../../actions)

## 🔗 External Resources

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [GitHub](https://github.com/JetBrains/compose-multiplatform/issues).

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.

## 🗄️ Database Setup

### View for listening to satr registration changes

```sql
create or replace view satr_registration_count as
select activity_id, count(*)::int as count
from satr_registration
group by activity_id
```
