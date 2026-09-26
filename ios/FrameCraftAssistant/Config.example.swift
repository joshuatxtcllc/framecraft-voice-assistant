import Foundation

// Copy this file to Config.swift (gitignored) and fill in real values.
// Keeping a tracked .example version means a fresh clone always shows
// exactly what needs to be set, without ever committing the real secret.
enum Config {
    static let backendBaseURL = URL(string: "https://YOUR-APP.up.railway.app")!
    static let assistantAPIKey = "YOUR_ASSISTANT_API_KEY"
}
