import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    // Removed Sentry initialization from here - now done inside Kotlin App()
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
