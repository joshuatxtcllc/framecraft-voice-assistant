import SwiftUI

@main
struct FrameCraftAssistantApp: App {
    // Bumped whenever a framecraftassistant://listen deep link arrives,
    // forcing ListeningView to reset and start a fresh listening session
    // even if the app was already open.
    @State private var sessionID = UUID()

    var body: some Scene {
        WindowGroup {
            ListeningView()
                .id(sessionID)
                .onOpenURL { url in
                    guard url.scheme == "framecraftassistant" else { return }
                    sessionID = UUID()
                }
        }
    }
}
