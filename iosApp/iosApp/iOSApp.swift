import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init(){
        SentrySetupKt.initializeSentry()
        KoinInitializer.shared.start { _ in }
        // Start session bootstrap
        AppBootstrap.shared.initialize()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// class AppDelegate: NSObject, UIApplicationDelegate {
//     func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
//         SentrySetupKt.initializeSentry()
//
//         return true
//     }
// }
