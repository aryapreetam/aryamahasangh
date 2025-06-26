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
2. Copy `secrets.properties.template` to `secrets.properties`
3. Fill in your configuration values in `secrets.properties`
4. Run the setup script: `./setup-secrets.sh`

## 🛠️ Build Commands

### Release Build

```bash
./gradlew :composeApp:assembleRelease
```

### Download GraphQL Schema

```bash
./gradlew downloadApolloSchema --endpoint="http://localhost:4000/graphql" --schema="composeApp/src/commonMain/graphql/schema.json"
```

## 📦 Release Management

This project uses automated semantic versioning for releases. Every push to the `dev` branch triggers a new release with
an incremented version number.

### Creating a New Release

#### Option 1: Automatic (Recommended)

Simply push your changes to the `dev` branch:

```bash
git checkout dev
git add .
git commit -m "Your changes"
git push origin dev
```

The GitHub Actions workflow will automatically:

1. Generate a new version number (incrementing patch version)
2. Build Android APK and Web distribution
3. Create a GitHub release with artifacts
4. Deploy web app to Netlify

#### Option 2: Manual Version Control

Use the version bump script for more control:

```bash
# Bump patch version (0.0.1 -> 0.0.2)
./version-bump.sh patch

# Bump minor version (0.0.1 -> 0.1.0)
./version-bump.sh minor

# Bump major version (0.0.1 -> 1.0.0)
./version-bump.sh major
```

### Version Numbering

- **Major**: Breaking changes or significant new features
- **Minor**: New features that are backward compatible
- **Patch**: Bug fixes and small improvements

### Release Artifacts

Each release includes:

- 📱 **AryaMahasangh_vX.X.X.apk** - Android application
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
