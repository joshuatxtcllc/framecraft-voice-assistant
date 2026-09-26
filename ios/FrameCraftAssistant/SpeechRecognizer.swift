import Foundation
import AVFoundation
import Speech

/// Wraps Apple's on-device Speech framework: records the mic, streams live
/// transcription, and reports final text when the user stops or goes quiet.
/// No audio ever leaves the phone for transcription — only the final text
/// is sent to the backend.
@MainActor
final class SpeechRecognizer: ObservableObject {
    enum State: Equatable {
        case idle
        case requestingPermission
        case listening
        case finished(String)
        case error(String)
    }

    @Published private(set) var state: State = .idle
    @Published private(set) var liveText: String = ""

    private let audioEngine = AVAudioEngine()
    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var silenceTimer: Timer?

    /// How long to wait after the user stops talking before treating the
    /// phrase as finished and sending it off.
    private let silenceTimeout: TimeInterval = 1.6

    func start() {
        state = .requestingPermission
        SFSpeechRecognizer.requestAuthorization { [weak self] authStatus in
            AVAudioApplication.requestRecordPermission { granted in
                Task { @MainActor in
                    guard let self else { return }
                    guard authStatus == .authorized, granted else {
                        self.state = .error("Microphone or speech recognition permission was denied.")
                        return
                    }
                    self.beginListening()
                }
            }
        }
    }

    func stopAndFinish() {
        finishRecognition()
    }

    private func beginListening() {
        guard let recognizer, recognizer.isAvailable else {
            state = .error("Speech recognizer unavailable right now.")
            return
        }

        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .measurement, options: [.duckOthers, .defaultToSpeaker])
            try session.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            state = .error("Couldn't start the audio session: \(error.localizedDescription)")
            return
        }

        let req = SFSpeechAudioBufferRecognitionRequest()
        req.shouldReportPartialResults = true
        req.requiresOnDeviceRecognition = false
        request = req

        let inputNode = audioEngine.inputNode
        let format = inputNode.outputFormat(forBus: 0)
        inputNode.removeTap(onBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: format) { [weak self] buffer, _ in
            self?.request?.append(buffer)
        }

        audioEngine.prepare()
        do {
            try audioEngine.start()
        } catch {
            state = .error("Couldn't start the audio engine: \(error.localizedDescription)")
            return
        }

        state = .listening
        liveText = ""

        task = recognizer.recognitionTask(with: req) { [weak self] result, error in
            guard let self else { return }
            Task { @MainActor in
                if let result {
                    self.liveText = result.bestTranscription.formattedString
                    self.resetSilenceTimer()
                    if result.isFinal {
                        self.finishRecognition()
                    }
                }
                if error != nil {
                    self.finishRecognition()
                }
            }
        }

        resetSilenceTimer()
    }

    private func resetSilenceTimer() {
        silenceTimer?.invalidate()
        silenceTimer = Timer.scheduledTimer(withTimeInterval: silenceTimeout, repeats: false) { [weak self] _ in
            Task { @MainActor in self?.finishRecognition() }
        }
    }

    private func finishRecognition() {
        silenceTimer?.invalidate()
        silenceTimer = nil

        guard state == .listening else { return }

        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        task?.cancel()

        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)

        let finalText = liveText.trimmingCharacters(in: .whitespacesAndNewlines)
        state = finalText.isEmpty ? .error("Didn't catch anything.") : .finished(finalText)

        request = nil
        task = nil
    }

    func reset() {
        state = .idle
        liveText = ""
    }
}
