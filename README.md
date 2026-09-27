# FrameCraft Voice Assistant

A custom voice assistant for Jay's Frames, in three flavors:

- **Path A — iOS app + widget:** tap a Home Screen/StandBy widget → speak →
  runs your request through Claude with live FrameKraft tools → speaks
  back the answer or does the action. One unlock + one tap, since iOS
  doesn't allow true background listening for third-party apps.
- **Path B (Pi) — dedicated always-on device:** a small Linux box
  (Raspberry Pi) on the shop counter, genuinely always listening for a
  wake word ("Hey Jarvis" out of the box, trainable to a custom word),
  no unlocking or tapping anything.
- **Path B (Android) — an old phone repurposed as the same thing:** a
  native Android app using a foreground service, so an old phone you
  already have can do the same always-on job as the Pi, with its own
  built-in mic/speaker/battery.

All three share one backend — same Claude call, same FrameKraft tools,
same source of truth — so nothing is duplicated between them.

| Folder | What it is | Where it runs |
|---|---|---|
| [`/server`](./server) | Express/TypeScript orchestrator — takes a transcript, calls Claude with the FrameKraft MCP tools attached, returns a reply | Railway (via GitHub auto-deploy, same pattern as the rest of the stack) |
| [`/ios`](./ios) | SwiftUI app + WidgetKit widget — on-device speech-to-text and text-to-speech, one-screen "tap and talk" UI | Sideloaded on your iPhone via Xcode |
| [`/listener`](./listener) | Python always-on wake-word service — openWakeWord detection, Whisper transcription, OpenAI TTS playback | Raspberry Pi (or similar) on the counter |
| [`/android`](./android) | Kotlin app — Porcupine wake-word detection in a foreground service, Android's built-in speech-to-text/text-to-speech | An old Android phone, plugged in, on the counter |

## Why it's split this way

- **The backend is the only place Claude + FrameKraft logic lives.** All
  three front ends just capture voice, send text, and play back the
  reply. No reasoning is duplicated.
- **iOS and Android each use their own platform's built-in voice I/O.**
  Apple's Speech framework/AVSpeechSynthesizer and Android's
  SpeechRecognizer/TextToSpeech both do transcription and speech output
  for free, on-device or via the OS's own service — no extra API keys
  for either.
- **The Pi listener uses OpenAI for voice I/O** instead, because a
  Pi-class CPU can't do real-time on-device transcription/TTS at usable
  quality — the wake-word detection itself *is* on-device and free
  (openWakeWord), only the transcription/speech steps go over the
  network.
- **Real tools, no mock data.** The server connects to your existing
  FrameKraft MCP server as a live tool source in the Claude API call —
  same orders, same customers, same inventory you already work from.

## Quickstart

1. **Deploy the backend first** — follow [`server/README.md`](./server/README.md).
   You'll end up with a Railway URL and an `ASSISTANT_API_KEY`.
2. **Pick a front end (or more than one):**
   - iOS app: follow [`ios/README.md`](./ios/README.md).
   - Raspberry Pi counter device: follow [`listener/README.md`](./listener/README.md).
   - Old Android phone as the counter device: follow [`android/README.md`](./android/README.md).

## Status

- **Backend (`/server`):** deployed and live on Railway, auth-gated,
  verified end to end with a real Claude turn. FrameKraft MCP tool
  connection is being wired in — see the repo's recent commits for the
  current URL/token being tested.
- **iOS app (`/ios`):** scaffold complete, not yet built/sideloaded onto
  a device.
- **Pi listener (`/listener`):** scaffold complete — real wake-word
  detection, transcription, and TTS, not a mock — not yet run on real
  hardware. Needs a Raspberry Pi (or similar) to test on.
- **Android app (`/android`):** scaffold complete — real Porcupine
  wake-word detection, Android's own STT/TTS, a real foreground service —
  not yet built/installed on the actual old phone.
