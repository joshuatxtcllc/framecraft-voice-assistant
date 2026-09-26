# FrameCraft Voice Assistant

A custom, hands-free-ish voice assistant for Jay's Frames: tap a Home
Screen/StandBy widget → speak → it runs your request through Claude with
live FrameKraft tools → speaks back the answer or does the action (send an
email, look up an order, etc.).

Two parts, two deploy targets:

| Folder | What it is | Where it runs |
|---|---|---|
| [`/server`](./server) | Express/TypeScript orchestrator — takes a transcript, calls Claude with the FrameKraft MCP tools attached, returns a reply | Railway (via GitHub auto-deploy, same pattern as the rest of the stack) |
| [`/ios`](./ios) | SwiftUI app + WidgetKit widget — on-device speech-to-text and text-to-speech, one-screen "tap and talk" UI | Sideloaded on your iPhone via Xcode |

## Why it's split this way

- **iOS handles voice, the server handles thinking.** Apple's Speech
  framework and AVSpeechSynthesizer do transcription and speech output
  on-device for free — no extra API keys, no audio ever leaves the phone
  except as text. The server's only job is running that text through
  Claude with your real tools.
- **No wake word.** iOS doesn't allow third-party apps to listen in the
  background with the screen off — that layer is reserved for Siri. This
  app trades that for a one-tap unlock + tap-the-icon flow, which is the
  most "hands-free" version actually buildable on iOS without a separate
  always-on listening device.
- **Real tools, no mock data.** The server connects to your existing
  FrameKraft MCP server as a live tool source in the Claude API call —
  same orders, same customers, same inventory you already work from.

## Quickstart

1. **Deploy the backend first** — follow [`server/README.md`](./server/README.md).
   You'll end up with a Railway URL and an `ASSISTANT_API_KEY`.
2. **Build the iOS app** — follow [`ios/README.md`](./ios/README.md), plugging
   in that URL and key.
3. Add the widget to your Home Screen or StandBy dock and start talking to it.

## Status

Initial scaffold — backend orchestrator and iOS app/widget shell are in
place and wired together. Not yet deployed or tested end-to-end. Next
real steps: confirm/expose FrameKraft's MCP server over a public HTTP URL
(see `server/README.md` §1), deploy the Railway service, build+sideload
the iOS app, and test one real voice request end to end.
