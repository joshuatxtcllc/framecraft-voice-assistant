# FrameCraft Voice Assistant — iOS App

A single-screen iOS app: tap the icon (or the widget), it starts listening
immediately, transcribes what you say on-device, sends the text to the
Railway backend (see `/server`), and speaks the reply back.

## What's here vs. what you build in Xcode

This repo has the **Swift source files**, not a full `.xcodeproj`. Xcode
project files are dense, machine-generated, and break easily when written
by hand outside Xcode — so the honest, reliable path is: create a fresh
Xcode project (2 minutes) and drop these files in, rather than trust a
hand-built `.pbxproj`. Steps below.

## One-time setup

### 1. Create the Xcode project

1. Xcode → File → New → Project → **iOS → App**.
2. Product Name: `FrameCraftAssistant`. Interface: **SwiftUI**. Language: **Swift**.
3. Uncheck "Use Core Data" / "Include Tests" (not needed).
4. Save it anywhere — you'll replace the generated files with the ones here.

### 2. Add the source files

Delete the placeholder `ContentView.swift` and `FrameCraftAssistantApp.swift`
Xcode generated. Drag the files from this repo's `ios/FrameCraftAssistant/`
folder into the Xcode project navigator (check "Copy items if needed").

### 3. Fill in your config

```bash
cp ios/FrameCraftAssistant/Config.example.swift ios/FrameCraftAssistant/Config.swift
```

Edit `Config.swift`:
- `backendBaseURL` → your Railway URL from `/server` setup (e.g.
  `https://framecraft-voice-assistant-production.up.railway.app`)
- `assistantAPIKey` → the exact `ASSISTANT_API_KEY` you set in Railway

`Config.swift` is gitignored on purpose — it holds your live secret.

### 4. Add the required Info.plist keys

Open your app target → **Info** tab and add the keys listed in
`Info-additions.plist` (microphone usage, speech recognition usage, and
the `framecraftassistant://` URL scheme). In modern Xcode projects these
are just rows you add in the Info tab UI, not a plist you edit by hand.

### 5. Add the Widget extension

1. File → New → Target → **Widget Extension**. Name it
   `FrameCraftAssistantWidget`. Uncheck "Include Live Activity" (not needed).
2. Replace the generated widget Swift file with
   `ios/FrameCraftAssistantWidget/FrameCraftAssistantWidget.swift` from this repo.
3. Make sure the widget extension target can see the `framecraftassistant://`
   scheme — it just needs the `Link(destination:)` URL, no special entitlement.

### 6. Sign and install on your phone

1. Select your Apple ID under **Signing & Capabilities** for both the app
   target and the widget extension target (use your personal Apple ID —
   a paid developer account isn't required for a device you own, just a
   7-day free-provisioning re-sign if you're not enrolled in the paid
   program).
2. Plug in your iPhone, select it as the run destination, hit **Run**.
3. First launch: iOS will ask for Face ID/Touch ID trust for the
   developer certificate — Settings → General → VPN & Device Management
   → trust your Apple ID.
4. Long-press the Home Screen → **Edit Home Screen** → **+** (top left) →
   find `FrameCraft Assistant` → add the widget to your Home Screen or to
   **StandBy** (drop the phone in a charging dock/stand, it rotates into
   StandBy automatically and shows any widgets you've added there).

### 7. Use it

- Tap the app icon or widget → Face ID/passcode unlock (iOS requires this
  for any app opening from Lock Screen/Home Screen — no way around that
  layer) → the app opens straight into "Listening…" → speak → it
  transcribes, sends to your backend, and speaks the reply back.
- Tap "Ask again" for another turn without leaving the app.

## Known limitations (real ones, not hidden)

- **Not hands-free from a fully locked screen.** As covered earlier, no
  third-party app gets Siri's always-on background listening. You unlock,
  then tap — that's the one required touch.
- **7-day re-signing** applies if you're on a free (non-paid) Apple
  Developer account — Xcode will nag you to re-run the build weekly. A
  $99/year developer account removes that.
- **No App Store distribution needed or attempted** — this is sideloaded
  for your own phone only, which is all a single-user shop tool needs.
- **If a request fails** (bad transcript, backend down, tool error), the
  app shows the error text plainly rather than pretending it worked — check
  Railway logs (`/server` README) if replies stop coming back.
