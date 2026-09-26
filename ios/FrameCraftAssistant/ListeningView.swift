import SwiftUI
import AVFoundation

/// The entire app is this one screen. Tapping the Home Screen icon or widget
/// lands here directly and listening starts immediately — no menu, no
/// home tab, matching the "tap and talk" behavior of the mic button in
/// Messages or Claude's own voice mode.
struct ListeningView: View {
    @StateObject private var recognizer = SpeechRecognizer()
    @State private var assistantReply: String?
    @State private var isThinking = false
    @State private var errorText: String?
    private let speaker = AVSpeechSynthesizer()

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            statusIcon
                .font(.system(size: 72))
                .foregroundStyle(.tint)

            Text(headline)
                .font(.title2.weight(.semibold))
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            if !recognizer.liveText.isEmpty {
                Text(recognizer.liveText)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            if let assistantReply {
                Text(assistantReply)
                    .font(.body)
                    .padding()
                    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    .padding(.horizontal)
            }

            if let errorText {
                Text(errorText)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Spacer()

            Button(action: restart) {
                Label("Ask again", systemImage: "mic.fill")
                    .font(.headline)
                    .padding()
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal)
            .padding(.bottom, 32)
        }
        .onAppear { recognizer.start() }
        .onChange(of: recognizer.state) { _, newState in
            handle(newState)
        }
    }

    private var statusIcon: some View {
        Group {
            switch recognizer.state {
            case .listening:
                Image(systemName: "waveform")
            case .requestingPermission:
                Image(systemName: "lock.circle")
            default:
                if isThinking {
                    Image(systemName: "ellipsis.circle")
                } else {
                    Image(systemName: "mic.circle.fill")
                }
            }
        }
    }

    private var headline: String {
        switch recognizer.state {
        case .idle: return "Starting…"
        case .requestingPermission: return "Waiting for permission…"
        case .listening: return "Listening…"
        case .finished: return isThinking ? "Thinking…" : (assistantReply ?? "")
        case .error(let message): return message
        }
    }

    private func handle(_ state: SpeechRecognizer.State) {
        guard case .finished(let transcript) = state else { return }
        Task { await send(transcript) }
    }

    private func send(_ transcript: String) async {
        isThinking = true
        errorText = nil
        do {
            let result = try await AssistantClient.send(transcript: transcript)
            assistantReply = result.reply
            speak(result.reply)
        } catch {
            errorText = error.localizedDescription
        }
        isThinking = false
    }

    private func speak(_ text: String) {
        guard !text.isEmpty else { return }
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        speaker.speak(utterance)
    }

    private func restart() {
        assistantReply = nil
        errorText = nil
        recognizer.reset()
        recognizer.start()
    }
}

#Preview {
    ListeningView()
}
