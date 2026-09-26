import Foundation

struct AssistantResponse: Decodable {
    let reply: String
    let toolCallsUsed: [String]
}

enum AssistantClientError: Error, LocalizedError {
    case badStatus(Int, String)
    case network(Error)

    var errorDescription: String? {
        switch self {
        case .badStatus(let code, let body):
            return "Server returned \(code): \(body)"
        case .network(let error):
            return error.localizedDescription
        }
    }
}

/// Talks to the Railway-hosted assistant backend. One call in, one reply out —
/// all the Claude + FrameKraft-tools work happens server-side.
enum AssistantClient {
    static func send(transcript: String) async throws -> AssistantResponse {
        var request = URLRequest(url: Config.backendBaseURL.appendingPathComponent("api/assistant/query"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(Config.assistantAPIKey, forHTTPHeaderField: "x-assistant-key")
        request.httpBody = try JSONEncoder().encode(["transcript": transcript])
        request.timeoutInterval = 30

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                throw AssistantClientError.badStatus(-1, "No HTTP response")
            }
            guard (200...299).contains(http.statusCode) else {
                let body = String(data: data, encoding: .utf8) ?? ""
                throw AssistantClientError.badStatus(http.statusCode, body)
            }
            return try JSONDecoder().decode(AssistantResponse.self, from: data)
        } catch let error as AssistantClientError {
            throw error
        } catch {
            throw AssistantClientError.network(error)
        }
    }
}
